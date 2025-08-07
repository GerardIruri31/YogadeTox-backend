package com.example.demo.qa.application;

import com.example.demo.qa.domain.QAService;
import com.example.demo.qa.dto.QAAResponseRequestDto;
import com.example.demo.qa.dto.QACreatedDTO;
import com.example.demo.qa.dto.QAResponseDto;
import com.example.demo.qa.domain.QA;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QAController {
    @Autowired
    private QAService qaService;

    // CLIENT CREA UNA PREGUNTA
    @PostMapping("/create")
    public ResponseEntity<QACreatedDTO> createQA(@RequestParam String message, @RequestParam Long clientId) {
        return ResponseEntity.ok(qaService.createQA(message, clientId));
    }

    // ADMIN OBTIENE UNA PREGUNTA POR SU ID
    @GetMapping("/{qaId}")
    public ResponseEntity<QACreatedDTO> getQAById(@PathVariable Long qaId) {
        return ResponseEntity.ok(qaService.getQAById(qaId));
    }

    // USER/ADMIN OBTIENE LIST QA POR CLIENTE ID
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<QACreatedDTO>> getQAsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(qaService.getQAsByClient(clientId));
    }

    // ADMIN OBTIENE PREGUNTAS SIN RESPONDER
    @GetMapping("/unresponded")
    public ResponseEntity<List<QACreatedDTO>> getUnrespondedQAs() {
        return ResponseEntity.ok(qaService.getUnrespondedQAs());
    }

    @PostMapping("/qa/{qaId}/respond")
    public ResponseEntity<QAResponseDto> respondToQA(@PathVariable Long qaId, @RequestBody QAAResponseRequestDto request) {
        return ResponseEntity.ok(qaService.respondToQA(qaId, request.getAdminId(), request.getMessage()));
    }
} 