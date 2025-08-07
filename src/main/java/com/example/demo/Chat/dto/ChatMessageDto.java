package com.example.demo.Chat.dto;

import com.example.demo.Chat.domain.SenderType;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ChatMessageDto {
    // ID del mensaje
    private Long id;
    // Contenido (message)
    private String content;
    // Fecha de creación del mensaje
    private ZonedDateTime timestamp;
    // Enum para saber proviene de CLIENT o ADMIN
    private SenderType senderType;
    // ID pregunta que corresponde
    private Long qaId;
    // ID (Client / Admin) que envió mensaje
    private Long senderId;
    // Nombre enviador
    private String senderName;
}
