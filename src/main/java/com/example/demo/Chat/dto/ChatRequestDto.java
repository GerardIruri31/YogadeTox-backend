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
    // ID del chat (opcional)
    private Long chatId;
    // Título de la conversación (opcional)
    private String conversationTitle;
    // Si es un nuevo chat
    private Boolean isNewChat;
}