package com.example.demo.Chat.application;

import com.example.demo.Chat.domain.ChatService;
import com.example.demo.Chat.dto.ChatMessageDto;
import com.example.demo.Chat.dto.ChatRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatRequestDto chatRequest, Principal principal) {
        if (chatRequest == null || chatRequest.getQaId() == null ||
                chatRequest.getMessage() == null || chatRequest.getMessage().isBlank()) {
            throw new IllegalArgumentException("qaId y message son obligatorios");
        }
        Long senderUserId = Long.valueOf(principal.getName());

        // Usar el método unificado
        chatService.sendMessageUnified(chatRequest.getQaId(), senderUserId, chatRequest.getMessage(), "TEXT");
    }

}