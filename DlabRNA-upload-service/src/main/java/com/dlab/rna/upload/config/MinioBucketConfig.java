package com.dlab.rna.upload.config;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class MinioBucketConfig {

    private final MinioClient minioClient;
    private final StorageProperties props;


    public MinioBucketConfig(MinioClient minioClient, StorageProperties props) {
        this.minioClient = minioClient;
        this.props = props;
    }

    @PostConstruct
    public void init() {
        try {
            String bucket = props.getMinio().getBucket();
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                System.out.println("Bucket '" + bucket + "' was created.");
            } else {
                System.out.println("Bucket '" + bucket + "' already exists.");
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO bucket initialization failed", e);
        }
    }
}
