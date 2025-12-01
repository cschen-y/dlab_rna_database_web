package com.dlab.rna.upload.mq;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.service.UploadService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Component
@Slf4j
public class MergeConsumer {
    private final UploadService service;
    private final RabbitTemplate rabbitTemplate;

    public MergeConsumer(UploadService service, RabbitTemplate rabbitTemplate) {
        this.service = service;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.MERGE_QUEUE, containerFactory = "manualAckContainerFactory")
    public void onMessage(@Payload String fileId,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("fileId: " + fileId + " deliveryTag: " + deliveryTag);
            service.compose(fileId);
            service.finishMerged(fileId);
            String object = service.getStoragePath(fileId);
            String payload = "{\"fileId\":\"" + fileId + "\",\"object\":\"" + object + "\"}";
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.SPLIT_QUEUE, payload);
            log.info("payload: " + payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            try {
                service.failMerged(fileId, e.getMessage());
                channel.basicAck(deliveryTag, false);
            } catch (Exception ignored) {}
        }
    }
}