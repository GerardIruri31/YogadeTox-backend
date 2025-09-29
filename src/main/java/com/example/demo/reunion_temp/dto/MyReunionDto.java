package com.example.demo.reunion_temp.dto;

import lombok.Data;

@Data
public class MyReunionDto {
    private String description;
    private String tag;
    private String sessionDate;
    private String horaInicio;
    private String horaFin;
    private Double cost;
    private Boolean isCancelled;
}