package com.example.demo.qa.dto;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class QACreatedDTO {
    private Long id;
    private ZonedDateTime createdAt;
    private boolean isResponded;
    private String message;
    private Long clientId;
    private String clientUsername;
}
