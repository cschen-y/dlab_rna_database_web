package com.dlab.rna.upload.mq;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.repo.VectorRepository;
import com.dlab.rna.upload.service.ChunkService;
import com.dlab.rna.upload.service.EmbeddingService;

import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.kafka.annotation.KafkaListener;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import com.dlab.rna.upload.config.StorageProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;


@Component
@Slf4j
public class ChunkConsumer {
    private final ChunkService chunkService;
    private final VectorRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MinioClient minioClient;
    private final StorageProperties props;
    private final KafkaTemplate<String, String> kafkaTemplate;


    public ChunkConsumer(ChunkService chunkService, VectorRepository repo, MinioClient minioClient, StorageProperties props, KafkaTemplate<String, String> kafkaTemplate, EmbeddingService embeddingService) {
        this.chunkService = chunkService;
        this.repo = repo;
        this.minioClient = minioClient;
        this.props = props;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void initPdfbox() {
        // 禁用字体扫描，避免 TTF 损坏导致 PDFBox 爆 EOFException
        System.setProperty("pdfbox.fontcache", "false");
    }

    @KafkaListener(topics = RabbitConfig.SPLIT_QUEUE, groupId = "split-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String message, org.springframework.kafka.support.Acknowledgment ack) {
        try {
            JsonNode root = mapper.readTree(message);

            String fileId = root.path("fileId").asText();
            String object = root.path("object").asText();

            int dot = object.lastIndexOf('.');
            String ext = (dot > 0 ? object.substring(dot + 1).toLowerCase() : "");

            log.info("DEBUG object={}, ext={}, isDoc={}", object, ext,
                    (ext.equals("txt") || ext.equals("md") || ext.equals("csv") || ext.equals("json")
                            || ext.equals("log") || ext.equals("pdf") || ext.equals("docx")));

            InputStream in = minioClient.getObject(
                    GetObjectArgs.builder().bucket(props.getMinio().getBucket()).object(object).build()
            );

            byte[] buf = in.readAllBytes();
            String text = "";

            switch (ext) {
                case "txt":
                case "md":
                case "csv":
                case "json":
                case "log":
                case "docx":
                    try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(buf))) {
                        try (XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
                            text = extractor.getText();
                        }
                    }
                    break;

                case "pdf":
                    try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(buf))) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        stripper.setSortByPosition(true);
                        text = stripper.getText(doc);
                    }
                    break;

                default:
                    log.warn("Unsupported file type: {}", ext);
                    return;
            }

            text = text.replace("\u0000", "")
                    .replaceAll("[\\x00-\\x1F]", " ")
                    .trim();

            for (ChunkService.Piece p : chunkService.split(fileId, object, text)) {
                String sha = chunkService.sha256(p.text);
                repo.upsertChunk(fileId, object, p.chunkId, p.text, p.offset, p.length, sha);
                Map<String, Object> payload = Map.of(
                        "fileId", fileId,
                        "object", object,
                        "chunkId", p.chunkId,
                        // "text", p.text,
                        "offset", p.offset,
                        "length", p.length
                );
                kafkaTemplate.send(RabbitConfig.CHUNKS_QUEUE, fileId, mapper.writeValueAsString(payload));
            }

            if (ack != null) ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing split message", e);
            throw new RuntimeException(e);
        }
    }
}
