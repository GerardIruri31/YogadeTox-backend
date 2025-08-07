package com.example.demo.Chat.dto;

import lombok.Data;

@Data
public class ChatRequestDto {
    // Contenido del message
    private String message;
    // ID del client
    private Long clientId;
    // ID del admin
    private Long adminId;
    // ID de la pregunta inicial
    private Long qaId;
}