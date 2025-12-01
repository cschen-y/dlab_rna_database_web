package com.dlab.rna.upload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ChunkService {
    @Value("${kb.chunk.size:1000}")
    private int maxSize;
    @Value("${kb.chunk.overlap:100}")
    private int overlapSentences;

    public List<Piece> split(String fileId, String object, String text) throws Exception {
        List<Piece> list = new ArrayList<>();
        if (text == null || text.isBlank()) return list;

        String unified = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] paragraphs = unified.split("(\n){2,}");

        int globalOffset = 0;
        for (String para : paragraphs) {
            String p = normalize(para);
            if (p.isEmpty()) continue;
            int length = p.length();
            int start = globalOffset;
            int end = start + length;
            String chunkId = hash(fileId + "|" + object + "|" + start + "|" + end);
            list.add(new Piece(chunkId, p, start, length));
            globalOffset += length;
        }

        return list;
    }

    private String normalize(String s) {
        String t = s.replace("\r\n", "\n").replace("\r", "\n");
        t = t.replaceAll("[\\t\\f\\u000B]+", " ");
        t = t.replaceAll("\n+", " ");
        t = t.replaceAll(" +", " ");
        t = t.trim();
        return t;
    }


    public String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String hash(String s) throws Exception {
        return sha256(s);
    }

    public static class Piece {
        public final String chunkId;
        public final String text;
        public final int offset;
        public final int length;
        public Piece(String chunkId, String text, int offset, int length) {
            this.chunkId = chunkId;
            this.text = text;
            this.offset = offset;
            this.length = length;
        }
    }
}