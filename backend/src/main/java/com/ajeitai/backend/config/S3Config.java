package com.ajeitai.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

import com.ajeitai.backend.service.storage.S3StorageConditionImpl;

/**
 * Configura o cliente S3 quando app.storage.type=s3.
 * Para Magalu Cloud Object Storage (se S3-compatível): definir app.storage.endpoint e região.
 */
@Configuration
@Conditional(S3StorageConditionImpl.class)
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${app.storage.region:us-east-1}") String region,
            @Value("${app.storage.endpoint:}") String endpoint) {
        var builder = S3Client.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
            builder.forcePathStyle(true);
        }
        return builder.build();
    }
}
