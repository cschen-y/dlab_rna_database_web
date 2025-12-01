package com.dlab.rna.upload.mq;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.service.EmbeddingService;
import com.rabbitmq.client.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingConsumer {
    private final EmbeddingService embeddingService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmbeddingConsumer(EmbeddingService embeddingService, RabbitTemplate rabbitTemplate) {
        this.embeddingService = embeddingService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.CHUNKS_QUEUE, containerFactory = "manualAckContainerFactory")
    public void onMessage(@Payload String message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            JsonNode root = mapper.readTree(message);
            String text = root.path("text").asText();
            EmbeddingService.Result r = embeddingService.embed(text, "test");
            if (!r.ok) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            try { channel.basicAck(deliveryTag, false); } catch (Exception ignored) {}
        }
    }
}