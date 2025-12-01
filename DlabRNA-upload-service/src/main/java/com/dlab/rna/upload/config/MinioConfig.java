package com.dlab.rna.upload.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Bean
    public MinioClient minioClient(StorageProperties props) {
        return MinioClient.builder()
                .endpoint(props.getMinio().getEndpoint())
                .credentials(props.getMinio().getAccessKey(), props.getMinio().getSecretKey())
                .build();
    }
}