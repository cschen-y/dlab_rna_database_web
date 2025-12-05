package com.dlab.rna.upload.mq;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.service.EmbeddingService;
import com.dlab.rna.upload.repo.VectorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmbeddingConsumer {
    private final EmbeddingService embeddingService;
    private final VectorRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmbeddingConsumer(EmbeddingService embeddingService, VectorRepository repo) {
        this.embeddingService = embeddingService;
        this.repo = repo;
    }

    @KafkaListener(topics = RabbitConfig.CHUNKS_QUEUE, groupId = "chunks-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String message, org.springframework.kafka.support.Acknowledgment ack) {
        try {
            JsonNode root = mapper.readTree(message);
            String fileId = root.path("fileId").asText();
            String object = root.path("object").asText();
            String chunkId = root.path("chunkId").asText();
            String text = repoFetchContent(fileId, object, chunkId);
            EmbeddingService.Result r = embeddingService.embed(text, "test");
            log.info("embedding result: {}", r);
            if (ack != null) ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing embedding message", e);
            throw new RuntimeException(e);
        }
    }

    private String repoFetchContent(String fileId, String object, String chunkId) {
        return repo.getChunkContent(fileId, object, chunkId);
    }
}