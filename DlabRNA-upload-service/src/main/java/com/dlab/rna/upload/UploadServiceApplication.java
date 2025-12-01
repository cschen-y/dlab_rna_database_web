package com.dlab.rna.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Upload服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UploadServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UploadServiceApplication.class, args);
    }
}
