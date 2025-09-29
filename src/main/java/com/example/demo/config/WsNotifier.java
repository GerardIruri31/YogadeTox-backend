package com.example.demo.config;

import com.example.demo.Chat.domain.ChatMessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WsNotifier {

    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(ChatMessageCreatedEvent event) {
        if (event.isPendingForAdmin()) {
            // Mensaje del CLIENTE → solo notificar a admins pendientes
            messaging.convertAndSend("/topic/qa/pending", Map.of("qaId", event.getQaId()));
            
            // Enviar ACK al cliente
            messaging.convertAndSendToUser(
                    event.getClientId().toString(),
                    "/queue/qa/ack",
                    Map.of("qaId", event.getQaId(), "messageId", event.getMessage().getId())
            );
        } else {
            // Mensaje del ADMIN → enviar solo al cliente específico
            messaging.convertAndSendToUser(
                    event.getClientId().toString(),
                    "/queue/qa/thread",
                    event.getMessage()
            );
        }
        
        // Broadcast general al chat (para admins que están viendo el chat)
        messaging.convertAndSend("/topic/qa/" + event.getQaId(), event.getMessage());
    }
}
