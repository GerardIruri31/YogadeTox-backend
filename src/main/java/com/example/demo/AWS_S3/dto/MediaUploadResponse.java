package com.example.demo.AWS_S3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {
    private boolean success;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String uploadMethod;
    private String errorMessage;
    private Long uploadTimeMs;
}