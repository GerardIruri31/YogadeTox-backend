package com.example.demo.Chat.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ConversationDto {
    private Long chatId;
    private String clientName;
    private String lastMessage;
    private ZonedDateTime lastMessageTime;
    private Boolean isActive;
    private Integer unreadCount;
    private Long clientId;
    private String conversationTitle;
}
