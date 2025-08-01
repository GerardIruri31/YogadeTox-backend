package com.example.demo.qa.domain;

import com.example.demo.qa.dto.QAResponseDto;
import com.example.demo.qa.infraestructure.QARepository;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QAService {
    
    private final QARepository qaRepository;
    private final ClientRepository clientRepository;
    private final ModelMapper modelMapper;

    public QA createQA(String message, Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        
        QA qa = new QA();
        qa.setMessage(message);
        qa.setClient(client);
        qa.setCreatedAt(ZonedDateTime.now());
        qa.setResponded(false);
        
        return qaRepository.save(qa);
    }

    public QA getQAById(Long qaId) {
        return qaRepository.findById(qaId)
                .orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
    }

    public List<QA> getQAsByClient(Long clientId) {
        return qaRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    public List<QA> getUnrespondedQAs() {
        return qaRepository.findByIsRespondedOrderByCreatedAtDesc(false);
    }

    public QA markAsResponded(Long qaId) {
        QA qa = getQAById(qaId);
        qa.setResponded(true);
        return qaRepository.save(qa);
    }

    public QAResponseDto convertToDto(QA qa) {
        QAResponseDto dto = modelMapper.map(qa, QAResponseDto.class);
        dto.setClientId(qa.getClient().getId());
        dto.setClientName(qa.getClient().getFirstName() + " " + qa.getClient().getLastName());

        if (qa.getChatMessages() != null && !qa.getChatMessages().isEmpty()) {
            dto.setChatMessages(qa.getChatMessages().stream()
                    .map(this::convertChatMessageToDto)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private com.example.demo.Chat.dto.ChatMessageDto convertChatMessageToDto(com.example.demo.Chat.domain.ChatMessageEntity chatMessage) {
        com.example.demo.Chat.dto.ChatMessageDto dto = modelMapper.map(chatMessage, com.example.demo.Chat.dto.ChatMessageDto.class);
        dto.setQaId(chatMessage.getQa().getId());
        
        if (chatMessage.getSenderType() == com.example.demo.Chat.domain.SenderType.CLIENT && chatMessage.getClient() != null) {
            dto.setSenderId(chatMessage.getClient().getId());
            dto.setSenderName(chatMessage.getClient().getFirstName() + " " + chatMessage.getClient().getLastName());
        } else if (chatMessage.getSenderType() == com.example.demo.Chat.domain.SenderType.ADMIN && chatMessage.getAdmin() != null) {
            dto.setSenderId(chatMessage.getAdmin().getId());
            dto.setSenderName(chatMessage.getAdmin().getFirstName() + " " + chatMessage.getAdmin().getLastName());
        }
        
        return dto;
    }
} 