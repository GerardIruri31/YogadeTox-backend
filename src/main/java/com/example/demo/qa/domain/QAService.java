package com.example.demo.qa.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.qa.dto.QACreatedDTO;
import com.example.demo.qa.dto.QAResponseDto;
import com.example.demo.qa.infraestructure.QARepository;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import com.example.demo.tbIntermediateAdminQa.infraestructure.QAAdminRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        QA qa = new QA();
        qa.setMessage(message);
        qa.setClient(client);
        qa.setCreatedAt(ZonedDateTime.now());
        qa.setResponded(false);
        QA saved = qaRepository.save(qa);
        QACreatedDTO dto = modelMapper.map(saved, QACreatedDTO.class);
        dto.setClientId(clientId);
        return dto;
    }

    public QACreatedDTO getQAById(Long qaId) {
        QA qa = qaRepository.findById(qaId)
                .orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
        QACreatedDTO dto = modelMapper.map(qa, QACreatedDTO.class);
        dto.setClientId(qa.getClient().getId());
        return dto;
    }

    public List<QACreatedDTO> getQAsByClient(Long clientId) {
       List<QA> qa_s = qaRepository.findByClientIdOrderByCreatedAtDesc(clientId);
       List<QACreatedDTO> dtos = new ArrayList<>();
       for (QA qa : qa_s) {
            QACreatedDTO dto = modelMapper.map(qa, QACreatedDTO.class);
            dto.setClientId(clientId);
            dtos.add(dto);
       }
       return dtos;
    }

    public List<QACreatedDTO> getUnrespondedQAs() {
        List<QA> qa_s = qaRepository.findByIsRespondedOrderByCreatedAtDesc(false);
        List<QACreatedDTO> dtos = new ArrayList<>();
        for (QA qa : qa_s) {
            QACreatedDTO dto = modelMapper.map(qa, QACreatedDTO.class);
            dto.setClientId(qa.getClient().getId());
            dtos.add(dto);
        }
        return dtos;
    }


    public QAResponseDto respondToQA(Long qaId, Long adminId, String message) {
        QA qa = qaRepository.findById(qaId)
                .orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));

        QAAdmin qaAdmin = qaAdminRepository.findByQaAndAdmin(qa, admin)
                .orElseGet(() -> {
                    QAAdmin newRel = new QAAdmin();
                    newRel.setQa(qa);
                    newRel.setAdmin(admin);
                    return qaAdminRepository.save(newRel);
                });

        qa.setResponded(true);
        qa.setMessage(message); // si tienes un campo para la respuesta
        qa.set(ZonedDateTime.now());
        qaRepository.save(qa);

        // 5. Mapear a DTO de respuesta
        QAResponseDto responseDto = convertToDto(qa, admin, message);

        // 6. (Opcional) Notificar por WebSocket

        return responseDto;
    }

}