package com.example.demo.qa.application;

import com.example.demo.qa.domain.QAService;
import com.example.demo.qa.dto.FullQAResponseDTO;
import com.example.demo.qa.dto.QAAResponseRequestDto;
import com.example.demo.qa.dto.QACreatedDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QAController {
    @Autowired
    private QAService qaService;

    // CLIENT CREA UNA PREGUNTA
    @PreAuthorize("hasRole('FREE')")
    @PostMapping("/create/{clientId}")
    public ResponseEntity<QACreatedDTO> createQA(@RequestParam String message, @PathVariable Long clientId) {
        return ResponseEntity.ok(qaService.createQA(message, clientId));
    }

    // CLIENT OBTIENE TODAS LAS PREGUNTAS
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<FullQAResponseDTO>> getAllQA() {
        return ResponseEntity.ok(qaService.getAllQA());
    }

    // ADMIN OBTIENE UNA PREGUNTA POR SU ID
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{qaId}")
    public ResponseEntity<FullQAResponseDTO> getQAById(@PathVariable Long qaId) {
        return ResponseEntity.ok(qaService.getQAById(qaId));
    }

    // ADMIN OBTIENE LIST QA POR CLIENTE ID - MODIFY RESPONSE DTO
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<FullQAResponseDTO>> getQAsByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(qaService.getQAsByClient(clientId));
    }

    // ADMIN OBTIENE PREGUNTAS SIN RESPONDER
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/unresponded")
    public ResponseEntity<List<QACreatedDTO>> getUnrespondedQAs() {
        return ResponseEntity.ok(qaService.getUnrespondedQAs());
    }

    // ADMIN RESPONDE UNA PREGUNTA
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{qaId}/respond")
    public ResponseEntity<FullQAResponseDTO> respondToQA(@PathVariable Long qaId, @RequestBody QAAResponseRequestDto request) {
        return ResponseEntity.ok(qaService.respondToQA(qaId, request.getAdminId(), request.getMessage()));
    }
} 