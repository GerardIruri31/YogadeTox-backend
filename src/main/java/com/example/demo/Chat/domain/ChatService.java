package com.example.demo.Chat.domain;

import com.example.demo.Chat.dto.ChatMessageDto;
import com.example.demo.Chat.dto.ChatRequestDto;
import com.example.demo.Chat.infrastructure.ChatMessageRepository;
import com.example.demo.admin.domain.Admin;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.qa.domain.QA;
import com.example.demo.qa.infraestructure.QARepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatRepository;
    private final QARepository qaRepository;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageDto saveMessage(ChatRequestDto requestDto) {
        QA qa;
        if (requestDto.getQaId() == null) {
            Client client = clientRepository.findById(requestDto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            
            qa = new QA();
            qa.setMessage(requestDto.getMessage());
            qa.setClient(client);
            qa.setCreatedAt(ZonedDateTime.now());
            qa.setResponded(false);
            qa = qaRepository.save(qa);
        } else {
            qa = qaRepository.findById(requestDto.getQaId())
                    .orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
        }

        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(requestDto.getMessage());
        messageEntity.setTimestamp(ZonedDateTime.now());
        messageEntity.setQa(qa);

        if (requestDto.getAdminId() != null) {
            Admin admin = adminRepository.findById(requestDto.getAdminId())
                    .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));
            messageEntity.setAdmin(admin);
            messageEntity.setSenderType(SenderType.ADMIN);
            qa.setResponded(true);
            qaRepository.save(qa);
        } else {
            Client client = clientRepository.findById(requestDto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            messageEntity.setClient(client);
            messageEntity.setSenderType(SenderType.CLIENT);
        }

        ChatMessageEntity savedMessage = chatRepository.save(messageEntity);
        ChatMessageDto responseDto = convertToDto(savedMessage);

        String destination = "/topic/qa/" + qa.getId();
        messagingTemplate.convertAndSend(destination, responseDto);

        return responseDto;
    }

    public List<ChatMessageDto> getChatHistory(Long qaId) {
        List<ChatMessageEntity> messages = chatRepository.findByQaIdOrderByTimestampAsc(qaId);
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<QA> getQAsByClient(Long clientId) {
        return qaRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    public List<QA> getUnrespondedQAs() {
        return qaRepository.findByIsRespondedOrderByCreatedAtDesc(false);
    }

    private ChatMessageDto convertToDto(ChatMessageEntity entity) {
        ChatMessageDto dto = modelMapper.map(entity, ChatMessageDto.class);
        dto.setQaId(entity.getQa().getId());
        
        if (entity.getSenderType() == SenderType.CLIENT && entity.getClient() != null) {
            dto.setSenderId(entity.getClient().getId());
            dto.setSenderName(entity.getClient().getFirstName() + " " + entity.getClient().getLastName());
        } else if (entity.getSenderType() == SenderType.ADMIN && entity.getAdmin() != null) {
            dto.setSenderId(entity.getAdmin().getId());
            dto.setSenderName(entity.getAdmin().getFirstName() + " " + entity.getAdmin().getLastName());
        }
        
        return dto;
    }
}