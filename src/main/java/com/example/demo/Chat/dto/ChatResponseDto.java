package com.example.demo.Chat.dto;

import lombok.Data;

@Data
public class ChatResponseDto {
    private Long chatId;
    private String message;
    private Boolean isNewChat;
    private String conversationTitle;
    private Long qaId;
}
