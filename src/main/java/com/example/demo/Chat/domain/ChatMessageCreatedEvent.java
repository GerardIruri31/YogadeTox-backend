package com.example.demo.Chat.domain;

import com.example.demo.Chat.dto.ChatMessageDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChatMessageCreatedEvent extends ApplicationEvent {
    private final Long qaId;
    private final ChatMessageDto message;
    private final Long clientId;
    private final boolean pendingForAdmin;

    public ChatMessageCreatedEvent(Long qaId, ChatMessageDto message, Long clientId, boolean pendingForAdmin) {
        super(qaId);
        this.qaId = qaId;
        this.message = message;
        this.clientId = clientId;
        this.pendingForAdmin = pendingForAdmin;
    }
}