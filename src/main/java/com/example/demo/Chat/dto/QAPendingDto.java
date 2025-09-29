package com.example.demo.Chat.dto;

// QAPendingDto.java
public record QAPendingDto(
        Long id,
        String message,
        String clientName,
        Long clientId
) {}
