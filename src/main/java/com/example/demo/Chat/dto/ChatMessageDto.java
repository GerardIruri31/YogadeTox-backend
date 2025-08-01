package com.example.demo.Chat.dto;

import com.example.demo.Chat.domain.SenderType;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ChatMessageDto {
    private Long id;
    private String content;
    private ZonedDateTime timestamp;
    private SenderType senderType;
    private Long qaId;
    private Long senderId;
    private String senderName;
}
