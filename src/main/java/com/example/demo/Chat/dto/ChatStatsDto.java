package com.example.demo.Chat.dto;

import lombok.Data;

@Data
public class ChatStatsDto {
    private Long totalConversations;
    private Long activeConversations;
    private Long pendingConversations;
    private Long totalMessages;
    private Long messagesToday;
    private Double averageResponseTime; // en minutos
    private Long totalClients;
    private Long activeClients;
}
