package com.example.demo.Chat.infrastructure;

import com.example.demo.Chat.domain.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByQaIdOrderByTimestampAsc(Long qaId);
    List<ChatMessageEntity> findByQaIdAndSenderTypeOrderByTimestampAsc(Long qaId, com.example.demo.Chat.domain.SenderType senderType);
}
