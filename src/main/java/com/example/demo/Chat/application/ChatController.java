package com.example.demo.Chat.application;

import com.example.demo.Chat.domain.ChatService;
import com.example.demo.Chat.dto.ChatMessageDto;
import com.example.demo.Chat.dto.ChatRequestDto;
import com.example.demo.qa.domain.QA;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<ChatMessageDto> sendMessage(@RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(chatService.saveMessage(request));
    }

    @GetMapping("/history/{qaId}")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long qaId) {
        return ResponseEntity.ok(chatService.getChatHistory(qaId));
    }

    @GetMapping("/qa/client/{clientId}")
    public ResponseEntity<List<QA>> getQAsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(chatService.getQAsByClient(clientId));
    }

    @GetMapping("/qa/unresponded")
    public ResponseEntity<List<QA>> getUnrespondedQAs() {
        return ResponseEntity.ok(chatService.getUnrespondedQAs());
    }
} 