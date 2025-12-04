package com.dlab.rna.upload.mq;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.repo.VectorRepository;
import com.dlab.rna.upload.service.ChunkService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.Producer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
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
import java.util.stream.Collectors;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Component
@Slf4j
public class ChunkConsumer {
    private final ChunkService chunkService;
    private final VectorRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MinioClient minioClient;
    private final StorageProperties props;
    private final Environment env;
    private Producer chunksProducer;

    public ChunkConsumer(ChunkService chunkService, VectorRepository repo, MinioClient minioClient, StorageProperties props, Environment env) {
        this.chunkService = chunkService;
        this.repo = repo;
        this.minioClient = minioClient;
        this.props = props;
        this.env = env;
    }

    @PostConstruct
    public void initPdfbox() {
        // 禁用字体扫描，避免 TTF 损坏导致 PDFBox 爆 EOFException
        System.setProperty("pdfbox.fontcache", "false");
    }

    @PostConstruct
    public void start() {
        try {
            env.streamCreator().stream(RabbitConfig.SPLIT_QUEUE).create();
        } catch (Throwable ignored) {
        }
        try {
            env.streamCreator().stream(RabbitConfig.CHUNKS_QUEUE).create();
        } catch (Throwable ignored) {
        }

        chunksProducer = env.producerBuilder().stream(RabbitConfig.CHUNKS_QUEUE).build();

        env.consumerBuilder()
                .stream(RabbitConfig.SPLIT_QUEUE)
                .name("split-consumer-group")
                .offset(com.rabbitmq.stream.OffsetSpecification.first())
                .messageHandler((ctx, msg) -> {

                    String message = null;
                    try {
                        message = new String(msg.getBodyAsBinary());
                        JsonNode root = mapper.readTree(message);

                        String fileId = root.path("fileId").asText();
                        String object = root.path("object").asText();

                        int dot = object.lastIndexOf('.');
                        String ext = (dot > 0 ? object.substring(dot + 1).toLowerCase() : "");

                        log.info("DEBUG object={}, ext={}, isDoc={}", object, ext,
                                (ext.equals("txt") || ext.equals("md") || ext.equals("csv") || ext.equals("json")
                                        || ext.equals("log") || ext.equals("pdf") || ext.equals("docx")));

                        // Read from MinIO
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
                                text = new String(buf, StandardCharsets.UTF_8);
                                break;

                            case "pdf":
                                ExecutorService executor = new ThreadPoolExecutor(
                                        10, 100, 1, TimeUnit.MINUTES,
                                        new LinkedBlockingQueue<>()
                                );
                                int numberOfPages;
                                try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(buf)))) {
                                    numberOfPages = pdfDoc.getNumberOfPages();
                                }

                                List<CompletableFuture<String>> futures = new ArrayList<>();

                                for (int i = 1; i <= numberOfPages; i++) {
                                    final int pageIndex = i;

                                    CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
                                        try (PdfDocument local =
                                                     new PdfDocument(new PdfReader(new ByteArrayInputStream(buf)))) {

                                            ITextExtractionStrategy strategy = new SimpleTextExtractionStrategy();

                                            return PdfTextExtractor.getTextFromPage(
                                                    local.getPage(pageIndex),
                                                    strategy
                                            );

                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }, executor);

                                    futures.add(cf);
                                }
                                text = futures.stream()
                                        .map(CompletableFuture::join)  // join 不需要处理 checked exception
                                        .collect(Collectors.joining());
                                break;
                            case "docx":
                                try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(buf))) {
                                    XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                                    text = extractor.getText();
                                }
                                break;

                            default:
                                log.warn("Unsupported file type: {}", ext);
                                return;
                        }

                        // Clean invalid chars
                        text = text.replace("\u0000", "")
                                .replaceAll("[\\x00-\\x1F]", " ")
                                .trim();

                        // split & insert
                        for (ChunkService.Piece p : chunkService.split(fileId, object, text)) {
                            String sha = chunkService.sha256(p.text);

                            repo.upsertChunk(fileId, object, p.chunkId, p.text, p.offset, p.length, sha);

                            Map<String, Object> payload = Map.of(
                                    "fileId", fileId,
                                    "object", object,
                                    "chunkId", p.chunkId,
                                    "text", p.text,
                                    "offset", p.offset,
                                    "length", p.length
                            );

                            chunksProducer.send(
                                    chunksProducer.messageBuilder().addData(mapper.writeValueAsBytes(payload)).build(),
                                    s -> {
                                    }
                            );
                        }

                    } catch (Exception e) {
                        log.error("Error processing message: {}", e.getMessage());
                    }

                    ctx.storeOffset();
                })
                .build();
    }
}
