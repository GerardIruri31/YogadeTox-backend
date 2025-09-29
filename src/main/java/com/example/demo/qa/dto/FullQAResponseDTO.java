package com.example.demo.qa.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class FullQAResponseDTO {
    private Long id;
    private ZonedDateTime createdAt;
    private boolean isResponded;
    private String message;
    private Long clientId;
    private String clientUsername;
    List<HistorialMessagesDTO> responses = new ArrayList<>();


    @Data
    public static class HistorialMessagesDTO {
        private Long adminId;
        private String response;
        private LocalDateTime respondedAt;
    }
}


