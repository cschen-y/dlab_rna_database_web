package com.dlab.rna.upload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private Minio minio = new Minio();
    private Chunk chunk = new Chunk();
    private Reconcile reconcile = new Reconcile();

    public Minio getMinio() { return minio; }
    public Chunk getChunk() { return chunk; }
    public Reconcile getReconcile() { return reconcile; }

    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
    }

    public static class Chunk {
        private int minSizeMb;
        private int maxSizeMb;

        public int getMinSizeMb() { return minSizeMb; }
        public void setMinSizeMb(int minSizeMb) { this.minSizeMb = minSizeMb; }
        public int getMaxSizeMb() { return maxSizeMb; }
        public void setMaxSizeMb(int maxSizeMb) { this.maxSizeMb = maxSizeMb; }
    }

    public static class Reconcile {
        private String cron;

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }
}