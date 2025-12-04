package com.dlab.rna.upload.service;

import com.dlab.rna.upload.config.StorageProperties;
import com.dlab.rna.upload.model.UploadTask;
import com.rabbitmq.stream.Environment;
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
    private final Environment env;

    public UploadService(MinioClient minioClient, StorageProperties props, StringRedisTemplate redis, UploadTaskMapper mapper, Environment env) {
        this.minioClient = minioClient;
        this.props = props;
        this.redis = redis;
        this.mapper = mapper;
        this.env = env;
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
        
        
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getMinio().getBucket())
                    .object(object)
                    .stream(in, file.getSize(), -1)
                    .contentType("application/octet-stream")
                    .build());
        }
        redis.opsForSet().add(keyParts(fileId), String.valueOf(index));
        return object;
    }

    public boolean canMerge(String fileId) {
        String state = redis.opsForValue().get(keyState(fileId));
        if (!"UPLOADING".equals(state)) return false;
        Object cc = redis.opsForHash().get(keyMeta(fileId), "chunkCount");
        int chunkCount = cc == null ? 0 : Integer.parseInt(cc.toString());
        Long done = redis.opsForSet().size(keyParts(fileId));
        return done != null && done.intValue() == chunkCount;
    }

    @Transactional
    public void markMerging(String fileId) {
        String lockKey = keyLock(fileId);
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey, "1");
        if (Boolean.FALSE.equals(ok)) return;
        redis.opsForValue().set(keyState(fileId), "MERGING");
    }

    @Transactional
    public void finishMerged(String fileId) {
        mapper.updateState(fileId, "COMPLETED", LocalDateTime.now());
        redis.opsForValue().set(keyState(fileId), "COMPLETED"); 
        redis.delete(keyLock(fileId));
        redis.delete(keyError(fileId));
    }

    @Transactional
    public void failMerged(String fileId, String message) {
        mapper.updateState(fileId, "FAILED", LocalDateTime.now());
        redis.opsForValue().set(keyState(fileId), "FAILED");
        if (org.springframework.util.StringUtils.hasText(message)) redis.opsForValue().set(keyError(fileId), message);
        redis.delete(keyLock(fileId));
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
        return redis.opsForValue().get(keyState(fileId));
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
        return cc == null ? 0 : Integer.parseInt(cc.toString());
    }

    public int chunkSize(String fileId) {
        Object cs = redis.opsForHash().get(keyMeta(fileId), "chunkSize");
        return cs == null ? 0 : Integer.parseInt(cs.toString());
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

    public void abort(String fileId) {
        redis.opsForValue().set(keyState(fileId), "CANCELLED");
    }

    private String keyMeta(String fileId) { return "upload:" + fileId + ":meta"; }
    private String keyParts(String fileId) { return "upload:" + fileId + ":parts"; }
    private String keyState(String fileId) { return "upload:" + fileId + ":state"; }
    private String keyLock(String fileId) { return "upload:" + fileId + ":mergelock"; }
    private String keySha(String sha256) { return "upload:sha:" + sha256; }
    private String keyError(String fileId) { return "upload:" + fileId + ":error"; }
}