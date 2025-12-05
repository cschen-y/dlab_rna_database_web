package com.dlab.rna.upload.service;

import com.dlab.rna.upload.config.StorageProperties;
import com.dlab.rna.upload.model.UploadTask;
import com.dlab.rna.upload.mapper.UploadTaskMapper;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UploadService {
    private final MinioClient minioClient;
    private final StorageProperties props;
    private final StringRedisTemplate redis;
    private final UploadTaskMapper mapper;

    public UploadService(MinioClient minioClient, StorageProperties props, StringRedisTemplate redis, UploadTaskMapper mapper) {
        this.minioClient = minioClient;
        this.props = props;
        this.redis = redis;
        this.mapper = mapper;
    }

    @Transactional
    public UploadTask init(String filename, long size, int chunkSize, String sha256) {
        String fileId = UUID.randomUUID().toString().replace("-", "");
        int chunkCount = (int) Math.ceil((double) size / (double) chunkSize);
        String objectName = fileId + "/merged/" + filename;
        UploadTask task = new UploadTask();
        task.setFileId(fileId);
        task.setFilename(filename);
        task.setSize(size);
        task.setChunkSize(chunkSize);
        task.setChunkCount(chunkCount);
        task.setSha256(sha256);
        task.setState("UPLOADING");
        task.setStoragePath(objectName);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        mapper.insert(task);
        String metaKey = keyMeta(fileId);
        redis.opsForHash().put(metaKey, "filename", filename);
        redis.opsForHash().put(metaKey, "size", String.valueOf(size));
        redis.opsForHash().put(metaKey, "chunkSize", String.valueOf(chunkSize));
        redis.opsForHash().put(metaKey, "chunkCount", String.valueOf(chunkCount));
        if (StringUtils.hasText(sha256)) redis.opsForHash().put(metaKey, "sha256", sha256);
        redis.opsForValue().set(keyState(fileId), "UPLOADING");
        if (StringUtils.hasText(sha256)) redis.opsForValue().set(keySha(sha256), fileId);
        return task;
    }

    public UploadTask initOrResume(String filename, long size, int chunkSize, String sha256) {
        if (StringUtils.hasText(sha256)) {
            String fid = redis.opsForValue().get(keySha(sha256));
            if (StringUtils.hasText(fid)) {
                UploadTask existing = mapper.findByFileId(fid);
                if (existing != null) return existing;
            }
        }
        return init(filename, size, chunkSize, sha256);
    }

    public String uploadChunk(String fileId, int index, MultipartFile file) throws Exception {
        String object = fileId + "/chunks/" + index;
        Boolean exists = redis.opsForSet().isMember(keyParts(fileId), String.valueOf(index));
        if (Boolean.TRUE.equals(exists)) return object;
        String lockKey = keyChunkLock(fileId, index);
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(locked)) {
            for (int i = 0; i < 60; i++) {
                Thread.sleep(500);
                Boolean done = redis.opsForSet().isMember(keyParts(fileId), String.valueOf(index));
                if (Boolean.TRUE.equals(done)) return object;
            }
            throw new IllegalStateException("chunk busy");
        }
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getMinio().getBucket())
                    .object(object)
                    .stream(in, file.getSize(), -1)
                    .contentType("application/octet-stream")
                    .build());
        }
        redis.opsForSet().add(keyParts(fileId), String.valueOf(index));
        redis.delete(lockKey);
        return object;
    }

    public boolean canMerge(String fileId) {
        String state = redis.opsForValue().get(keyState(fileId));
        if (!org.springframework.util.StringUtils.hasText(state)) {
            UploadTask t = mapper.findByFileId(fileId);
            state = t == null ? null : t.getState();
            if (org.springframework.util.StringUtils.hasText(state)) redis.opsForValue().set(keyState(fileId), state);
        }
        if (!"UPLOADING".equals(state)) return false;
        Object cc = redis.opsForHash().get(keyMeta(fileId), "chunkCount");
        int chunkCount = cc == null ? 0 : Integer.parseInt(cc.toString());
        if (chunkCount == 0) {
            UploadTask t = mapper.findByFileId(fileId);
            if (t != null && t.getChunkCount() != null) {
                chunkCount = t.getChunkCount();
                redis.opsForHash().put(keyMeta(fileId), "chunkCount", String.valueOf(chunkCount));
            }
        }
        Long done = redis.opsForSet().size(keyParts(fileId));
        return done != null && done.intValue() == chunkCount;
    }

    @Transactional
    public void markMerging(String fileId) {
        String lockKey = keyLock(fileId);
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey, "1");
        if (Boolean.FALSE.equals(ok)) return;
        mapper.updateState(fileId, "MERGING", LocalDateTime.now());
        redis.opsForValue().set(keyState(fileId), "MERGING");
    }

    @Transactional
    public void finishMerged(String fileId) {
        mapper.updateState(fileId, "COMPLETED", LocalDateTime.now());
        redis.delete(keyLock(fileId));
        try { redis.delete(keyState(fileId)); } catch (Exception ignored) {}
        try { redis.delete(keyError(fileId)); } catch (Exception ignored) {}
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            try { redis.delete(keyState(fileId)); } catch (Exception ignored) {}
            try { redis.delete(keyError(fileId)); } catch (Exception ignored) {}
        });
    }

    @Transactional
    public void failMerged(String fileId, String message) {
        mapper.updateState(fileId, "FAILED", LocalDateTime.now());
        if (org.springframework.util.StringUtils.hasText(message)) redis.opsForValue().set(keyError(fileId), message);
        redis.delete(keyLock(fileId));
        try { redis.delete(keyState(fileId)); } catch (Exception ignored) {}
        try { redis.delete(keyError(fileId)); } catch (Exception ignored) {}
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            try { redis.delete(keyState(fileId)); } catch (Exception ignored) {}
            try { redis.delete(keyError(fileId)); } catch (Exception ignored) {}
        });
    }

    public void compose(String fileId) throws Exception {
        UploadTask task = mapper.findByFileId(fileId);
        if (task == null) return;
        String partsKey = keyParts(fileId);
        int chunkCount = task.getChunkCount();
        List<ComposeSource> sources = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            sources.add(ComposeSource.builder()
                    .bucket(props.getMinio().getBucket())
                    .object(fileId + "/chunks/" + i)
                    .build());
        }
        minioClient.composeObject(ComposeObjectArgs.builder()
                .bucket(props.getMinio().getBucket())
                .object(task.getStoragePath())
                .sources(sources)
                .build());
    }

    public String getState(String fileId) {
        String v = redis.opsForValue().get(keyState(fileId));
        if (!org.springframework.util.StringUtils.hasText(v)) {
            UploadTask t = mapper.findByFileId(fileId);
            v = t == null ? null : t.getState();
            if (org.springframework.util.StringUtils.hasText(v)) redis.opsForValue().set(keyState(fileId), v);
        }
        return v;
    }

    public String getErrorMessage(String fileId) {
        return redis.opsForValue().get(keyError(fileId));
    }

    public String getStoragePath(String fileId) {
        UploadTask t = mapper.findByFileId(fileId);
        return t == null ? null : t.getStoragePath();
    }

    public int doneChunks(String fileId) {
        Long size = redis.opsForSet().size(keyParts(fileId));
        return size == null ? 0 : size.intValue();
    }

    public int totalChunks(String fileId) {
        Object cc = redis.opsForHash().get(keyMeta(fileId), "chunkCount");
        int v = cc == null ? 0 : Integer.parseInt(cc.toString());
        if (v == 0) {
            UploadTask t = mapper.findByFileId(fileId);
            if (t != null && t.getChunkCount() != null) {
                v = t.getChunkCount();
                redis.opsForHash().put(keyMeta(fileId), "chunkCount", String.valueOf(v));
            }
        }
        return v;
    }

    public int chunkSize(String fileId) {
        Object cs = redis.opsForHash().get(keyMeta(fileId), "chunkSize");
        int v = cs == null ? 0 : Integer.parseInt(cs.toString());
        if (v == 0) {
            UploadTask t = mapper.findByFileId(fileId);
            if (t != null && t.getChunkSize() != null) {
                v = t.getChunkSize();
                redis.opsForHash().put(keyMeta(fileId), "chunkSize", String.valueOf(v));
            }
        }
        return v;
    }

    public java.util.List<Integer> uploadedIndices(String fileId) {
        java.util.Set<String> members = redis.opsForSet().members(keyParts(fileId));
        java.util.List<Integer> list = new java.util.ArrayList<>();
        if (members != null) {
            for (String m : members) list.add(Integer.parseInt(m));
            list.sort(java.util.Comparator.naturalOrder());
        }
        return list;
    }

    public java.util.List<Integer> missingIndices(String fileId) {
        java.util.List<Integer> uploaded = uploadedIndices(fileId);
        int total = totalChunks(fileId);
        java.util.Set<Integer> set = new java.util.HashSet<>(uploaded);
        java.util.List<Integer> list = new java.util.ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (!set.contains(i)) list.add(i);
        }
        return list;
    }

    public void abort(String fileId) {
        mapper.updateState(fileId, "CANCELLED", LocalDateTime.now());
        try { redis.delete(keyState(fileId)); } catch (Exception ignored) {}
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            try { redis.delete(keyState(fileId)); } catch (Exception ignored) {}
        });
    }

    private String keyMeta(String fileId) { return "upload:" + fileId + ":meta"; }
    private String keyParts(String fileId) { return "upload:" + fileId + ":parts"; }
    private String keyState(String fileId) { return "upload:" + fileId + ":state"; }
    private String keyLock(String fileId) { return "upload:" + fileId + ":mergelock"; }
    private String keySha(String sha256) { return "upload:sha:" + sha256; }
    private String keyError(String fileId) { return "upload:" + fileId + ":error"; }
    private String keyChunkLock(String fileId, int index) { return "upload:" + fileId + ":chunklock:" + index; }
}