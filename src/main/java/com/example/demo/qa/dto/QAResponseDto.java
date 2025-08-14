package com.example.demo.qa.dto;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
public class QAResponseDto {
    private Long id;
    private String message;
    private ZonedDateTime createdAt;
    private boolean isResponded;
    private Long clientId;
    private String clientName;
}