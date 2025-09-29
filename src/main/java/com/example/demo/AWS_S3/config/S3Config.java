package com.example.demo.AWS_S3.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);
    @Value("${aws.access.key}")
    private String awsAccessKey;
    @Value("${aws.secret.key}")
    private String awsSecretKey;
    @Value("${aws.s3.region}")
    private String region;


    @Bean
    public S3Client getS3client(){
        try {
            AwsBasicCredentials basicCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
            S3Client client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
                    .build();
            logger.info("S3Client creado exitosamente para región: {}", region);
            return client;
        } catch (Exception e) {
            logger.error("Error creando S3Client: {}", e.getMessage());
            throw new RuntimeException("No se pudo crear S3Client", e);
        }
    }

    @Bean
    public S3Presigner getS3Presigner(){
        AwsBasicCredentials basicCredentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(basicCredentials))
                .build();
    }
}