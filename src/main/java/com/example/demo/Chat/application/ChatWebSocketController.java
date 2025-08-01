package com.example.demo.Chat.application;

import com.example.demo.Chat.domain.ChatService;
import com.example.demo.Chat.dto.ChatMessageDto;
import com.example.demo.Chat.dto.ChatRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/chat")
    public ChatMessageDto sendMessage(@Payload ChatRequestDto chatRequest) {
        return chatService.saveMessage(chatRequest);
    }

    @MessageMapping("/chat.joinQA")
    public void joinQA(@Payload Long qaId) {
    }
} 