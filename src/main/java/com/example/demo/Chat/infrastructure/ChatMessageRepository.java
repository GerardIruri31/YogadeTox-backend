package com.example.demo.Chat.infrastructure;

import com.example.demo.Chat.domain.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByQaIdOrderByTimestampAsc(Long qaId);
    List<ChatMessageEntity> findByQaIdAndSenderTypeOrderByTimestampAsc(Long qaId, com.example.demo.Chat.domain.SenderType senderType);
    
    // Nuevas consultas para conversaciones
    ChatMessageEntity findFirstByQaIdOrderByTimestampDesc(Long qaId);
    Long countByQaIdAndSenderType(Long qaId, com.example.demo.Chat.domain.SenderType senderType);
    List<ChatMessageEntity> findByQaIdOrderByTimestampDesc(Long qaId);
    
    // Consulta para traer solo los últimos mensajes (más recientes primero, luego invertir)
    List<ChatMessageEntity> findTop50ByQaIdOrderByTimestampDesc(Long qaId);
    
    // Consultas de conteo
    Long countByTimestampAfter(java.time.ZonedDateTime timestamp);
}
