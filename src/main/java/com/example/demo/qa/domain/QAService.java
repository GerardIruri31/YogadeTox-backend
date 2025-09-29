package com.example.demo.qa.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.qa.dto.FullQAResponseDTO;
import com.example.demo.qa.dto.QACreatedDTO;
import com.example.demo.qa.infraestructure.QARepository;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import com.example.demo.tbIntermediateAdminQa.infraestructure.QAAdminRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QAService {
    @Autowired
    private QARepository qaRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private QAAdminRepository qaAdminRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ModelMapper modelMapper;


    public QACreatedDTO createQA(String message, Long clientId) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Mensaje vacío");
        }
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clientId));
        QA qa = new QA();
        qa.setMessage(message);
        qa.setClient(client);
        qa.setCreatedAt(ZonedDateTime.now());
        qa.setResponded(false);
        QA saved = qaRepository.save(qa);
        QACreatedDTO dto = modelMapper.map(saved, QACreatedDTO.class);
        dto.setClientId(clientId);
        dto.setClientUsername(client.getUsername());
        return dto;
    }


    public List<FullQAResponseDTO> getAllQA() {
        List<QA> qa_s = qaRepository.findAll();
        List<FullQAResponseDTO> responses = new ArrayList<>();
        for (QA qa : qa_s) {
            FullQAResponseDTO dto = modelMapper.map(qa, FullQAResponseDTO.class);
            dto.setClientUsername(qa.getClient().getUsername());
            dto.setClientId(qa.getClient().getId());
            List<QAAdmin> responsesList = qaAdminRepository.findByQaIdOrderByRespondedAtAsc(qa.getId());
            if (!responsesList.isEmpty()) {
                List<FullQAResponseDTO.HistorialMessagesDTO> historialMessages = new ArrayList<>();
                responsesList.forEach(iter -> {
                    FullQAResponseDTO.HistorialMessagesDTO responseDTO = modelMapper.map(iter,FullQAResponseDTO.HistorialMessagesDTO.class);
                    responseDTO.setAdminId(iter.getAdmin().getId());
                    historialMessages.add(responseDTO);
                });
                dto.setResponses(historialMessages);
            }
            responses.add(dto);
        }
        return responses;
    }

    public FullQAResponseDTO getQAById(Long qaId) {
        QA qa = qaRepository.findById(qaId).orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
        FullQAResponseDTO dto = modelMapper.map(qa, FullQAResponseDTO.class);
        dto.setClientUsername(qa.getClient().getUsername());
        dto.setClientId(qa.getClient().getId());
        List<QAAdmin> responsesList = qaAdminRepository.findByQaIdOrderByRespondedAtAsc(qa.getId());
        if (!responsesList.isEmpty()) {
            List<FullQAResponseDTO.HistorialMessagesDTO> historialMessages = new ArrayList<>();
            responsesList.forEach(iter -> {
                FullQAResponseDTO.HistorialMessagesDTO responseDTO = modelMapper.map(iter,FullQAResponseDTO.HistorialMessagesDTO.class);
                responseDTO.setAdminId(iter.getAdmin().getId());
                historialMessages.add(responseDTO);
            });
            dto.setResponses(historialMessages);
        }
        return dto;
    }

    public List<FullQAResponseDTO> getQAsByClient(Long clientId) {
       List<QA> qa_s = qaRepository.findByClientIdOrderByCreatedAtDesc(clientId);
       List<FullQAResponseDTO> responses = new ArrayList<>();
       for (QA qa : qa_s) {
           FullQAResponseDTO dto = modelMapper.map(qa, FullQAResponseDTO.class);
           dto.setClientUsername(qa.getClient().getUsername());
           dto.setClientId(qa.getClient().getId());
           List<QAAdmin> responsesList = qaAdminRepository.findByQaIdOrderByRespondedAtAsc(qa.getId());
           if (!responsesList.isEmpty()) {
               List<FullQAResponseDTO.HistorialMessagesDTO> historialMessages = new ArrayList<>();
               responsesList.forEach(iter -> {
                   FullQAResponseDTO.HistorialMessagesDTO responseDTO = modelMapper.map(iter,FullQAResponseDTO.HistorialMessagesDTO.class);
                   responseDTO.setAdminId(iter.getAdmin().getId());
                   historialMessages.add(responseDTO);
               });
               dto.setResponses(historialMessages);
           }
           responses.add(dto);
       }
       return responses;
    }

    public List<QACreatedDTO> getUnrespondedQAs() {
        List<QA> qa_s = qaRepository.findByIsRespondedOrderByCreatedAtDesc(false);
        List<QACreatedDTO> dtos = new ArrayList<>();
        for (QA qa : qa_s) {
            QACreatedDTO dto = modelMapper.map(qa, QACreatedDTO.class);
            dto.setClientId(qa.getClient().getId());
            dto.setClientUsername(qa.getClient().getUsername());
            dtos.add(dto);
        }
        return dtos;
    }


    @Transactional
    public FullQAResponseDTO respondToQA(Long qaId, Long adminId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Mensaje vacío");
        }
        QA qa = qaRepository.findById(qaId).orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));
        QAAdmin qaAdmin = new QAAdmin();
        qaAdmin.setAdmin(admin);
        qaAdmin.setQa(qa);
        qaAdmin.setResponse(message);
        qaAdmin.setRespondedAt(LocalDateTime.now());
        if (!qa.isResponded()) {
            qa.setResponded(true);
        }
        qaAdminRepository.save(qaAdmin);
        QA savedQA = qaRepository.save(qa);
        // Mapear a DTO de respuesta
        FullQAResponseDTO responseDto = modelMapper.map(qa,FullQAResponseDTO.class);
        responseDto.setClientId(savedQA.getClient().getId());
        responseDto.setClientUsername(savedQA.getClient().getUsername());
        List<FullQAResponseDTO.HistorialMessagesDTO> historialMessages = new ArrayList<>();
        List<QAAdmin> responsesList = qaAdminRepository.findByQaIdOrderByRespondedAtAsc(qa.getId());
        responsesList.forEach(iter -> {
            FullQAResponseDTO.HistorialMessagesDTO responseDTO = modelMapper.map(iter,FullQAResponseDTO.HistorialMessagesDTO.class);
            responseDTO.setAdminId(iter.getAdmin().getId());
            historialMessages.add(responseDTO);
        });
        responseDto.setResponses(historialMessages);
        return responseDto;
    }
}