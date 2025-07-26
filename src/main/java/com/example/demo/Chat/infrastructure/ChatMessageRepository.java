package com.example.demo.Chat.infrastructure;

import com.example.demo.Chat.domain.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
}
