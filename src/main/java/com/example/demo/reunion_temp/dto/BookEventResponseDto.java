package com.example.demo.reunion_temp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookEventResponseDto {
    private Long id;
    private String url;
    private String description;
    private String tag;
    private LocalDateTime sesionDate;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Double cost;
    private Boolean isCancelled;
    private String clientName;
    private String clientEmail;
    private String message;
}