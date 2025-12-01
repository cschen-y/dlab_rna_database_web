package com.dlab.rna.upload.mq;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.repo.VectorRepository;
import com.dlab.rna.upload.service.ChunkService;
import com.rabbitmq.client.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import com.dlab.rna.upload.config.StorageProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ChunkConsumer {
    private final ChunkService chunkService;
    private final RabbitTemplate rabbitTemplate;
    private final VectorRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MinioClient minioClient;
    private final StorageProperties props;

    public ChunkConsumer(ChunkService chunkService, RabbitTemplate rabbitTemplate, VectorRepository repo, MinioClient minioClient, StorageProperties props) {
        this.chunkService = chunkService;
        this.rabbitTemplate = rabbitTemplate;
        this.repo = repo;
        this.minioClient = minioClient;
        this.props = props;
    }

    @RabbitListener(queues = RabbitConfig.SPLIT_QUEUE, containerFactory = "manualAckContainerFactory")
    public void onMessage(@Payload String message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            JsonNode root = mapper.readTree(message);
            String fileId = root.path("fileId").asText();
            String object = root.path("object").asText();
            String ext = "";
            int dot = object.lastIndexOf('.');
            if (dot > -1 && dot < object.length() - 1) ext = object.substring(dot + 1).toLowerCase();
            boolean isDoc = ext.equals("txt") || ext.equals("md") || ext.equals("csv") || ext.equals("json") || ext.equals("log");
            if (!isDoc) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            InputStream in = minioClient.getObject(GetObjectArgs.builder().bucket(props.getMinio().getBucket()).object(object).build());
            byte[] buf;
            try {
                buf = in.readAllBytes();
            } catch (Throwable t) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int r;
                while ((r = in.read(b)) != -1) baos.write(b, 0, r);
                buf = baos.toByteArray();
            }
            String text = new String(buf, StandardCharsets.UTF_8).replaceAll("\\\\n", " ");
            for (ChunkService.Piece p : chunkService.split(fileId, object, text)) {
                String sha = chunkService.sha256(p.text);
                repo.upsertChunk(fileId, object, p.chunkId, p.text, p.offset, p.length, sha);
                Map<String, Object> payload = new HashMap<>();
                payload.put("fileId", fileId);
                payload.put("object", object);
                payload.put("chunkId", p.chunkId);
                payload.put("text", p.text);
                payload.put("offset", p.offset);
                payload.put("length", p.length);
                String json = mapper.writeValueAsString(payload);
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.CHUNKS_QUEUE, json);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            try { channel.basicAck(deliveryTag, false); } catch (Exception ignored) {}
        }
    }
}