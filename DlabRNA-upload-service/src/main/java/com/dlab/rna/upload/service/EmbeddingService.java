package com.dlab.rna.upload.service;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

@Service
public class EmbeddingService {
    @Resource
    private PgVectorStore vectorStore;
    @Resource
    private TokenTextSplitter tokenTextSplitter;


    public Result embed(String text, String tag){
        try {
            Document one = new Document(text);
            List<Document> rawList  = List.of(one);
            List<Document> documentList = tokenTextSplitter.apply(rawList);

            // 添加知识库标签
            documentList.forEach(doc -> doc.getMetadata().put("ai-rag-knowledge", tag));

            // 存储知识库文件
            vectorStore.accept(documentList);

            return Result.ok();
        }
        catch (Exception e){
            return  Result.error("0001",e.getMessage());
        }
    }

    public static class Result {
        public final boolean ok;
        public final List<Double> embedding;
        public final String model;
        public final String errorCode;
        public final String errorMessage;
        private Result(boolean ok, List<Double> embedding, String model, String errorCode, String errorMessage) {
            this.ok = ok;
            this.embedding = embedding;
            this.model = model;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
        public static Result ok() { return new Result(true, null, null, null, null); }
        public static Result ok(List<Double> embedding, String model) { return new Result(true, embedding, model, null, null); }
        public static Result error(String code, String message) { return new Result(false, null, null, code, message); }
    }
}