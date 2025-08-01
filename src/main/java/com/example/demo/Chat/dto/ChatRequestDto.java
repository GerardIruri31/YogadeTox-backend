package com.example.demo.Chat.dto;

import lombok.Data;

@Data
public class ChatRequestDto {
    private String message;
    private Long clientId;
    private Long adminId;
    private Long qaId;
} 