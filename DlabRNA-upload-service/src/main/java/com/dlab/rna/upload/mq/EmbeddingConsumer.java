package com.dlab.rna.upload.mq;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.service.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.stream.Environment;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmbeddingConsumer {
    private final EmbeddingService embeddingService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Environment env;

    public EmbeddingConsumer(EmbeddingService embeddingService, Environment env) {
        this.embeddingService = embeddingService;
        this.env = env;
    }

    @PostConstruct
    public void start() {
        try { env.streamCreator().stream(RabbitConfig.CHUNKS_QUEUE).create(); } catch (Throwable ignored) {}
        env.consumerBuilder()
                .stream(RabbitConfig.CHUNKS_QUEUE)
                .name("chunks-consumer-group")
                .offset(com.rabbitmq.stream.OffsetSpecification.first())
                .messageHandler((ctx, msg) -> {
                    try {
                        String message = new String(msg.getBodyAsBinary());
                        JsonNode root = mapper.readTree(message);
                        String text = root.path("text").asText();
                        EmbeddingService.Result r = embeddingService.embed(text, "test");
                        log.info("embedding result: {}", r);
                    } catch (Exception ignored) {}
                    ctx.storeOffset();
                })
                .build();
    }
}