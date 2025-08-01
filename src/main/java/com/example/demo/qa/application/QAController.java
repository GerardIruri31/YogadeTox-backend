package com.example.demo.qa.application;

import com.example.demo.qa.domain.QAService;
import com.example.demo.qa.dto.QAResponseDto;
import com.example.demo.qa.domain.QA;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QAController {
    
    private final QAService qaService;

    @PostMapping("/create")
    public ResponseEntity<QA> createQA(@RequestParam String message, @RequestParam Long clientId) {
        return ResponseEntity.ok(qaService.createQA(message, clientId));
    }

    @GetMapping("/{qaId}")
    public ResponseEntity<QAResponseDto> getQAById(@PathVariable Long qaId) {
        QA qa = qaService.getQAById(qaId);
        return ResponseEntity.ok(qaService.convertToDto(qa));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<QA>> getQAsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(qaService.getQAsByClient(clientId));
    }

    @GetMapping("/unresponded")
    public ResponseEntity<List<QA>> getUnrespondedQAs() {
        return ResponseEntity.ok(qaService.getUnrespondedQAs());
    }

    @PatchMapping("/{qaId}/respond")
    public ResponseEntity<QA> markAsResponded(@PathVariable Long qaId) {
        return ResponseEntity.ok(qaService.markAsResponded(qaId));
    }
} 