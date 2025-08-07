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

    // Por cada mensaje enviado:
    public ChatMessageDto saveMessage(ChatRequestDto requestDto) {
        QA qa;
        if (requestDto.getQaId() == null) {
            // No existe QA previa, es la PRIMERA pregunta de un cliente
            Client client = clientRepository.findById(requestDto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            // Crea un nuevo QA con el mensaje del cliente
            qa = new QA();
            qa.setMessage(requestDto.getMessage());
            qa.setClient(client);
            qa.setCreatedAt(ZonedDateTime.now());
            qa.setResponded(false);
            // Guarda el QA en la base de datos
            qa = qaRepository.save(qa);
        } else {
            // Ya existe una QA previa (el mensaje es respuesta o continuación)
            qa = qaRepository.findById(requestDto.getQaId())
                    .orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
        }

        // Crea un objeto de mensaje de chat para guardar en la BD, con contenido, hora y referencia a la QA
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(requestDto.getMessage());
        messageEntity.setTimestamp(ZonedDateTime.now());
        messageEntity.setQa(qa);

        // Determina quién es el emisor del mensaje (cliente o admin)
        if (requestDto.getAdminId() != null) {
            // Mensaje enviado por un ADMIN
            Admin admin = adminRepository.findById(requestDto.getAdminId())
                    .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));
            messageEntity.setAdmin(admin);
            // Marca QA como respondida por el admin
            messageEntity.setSenderType(SenderType.ADMIN);
            qa.setResponded(true);
            qaRepository.save(qa);
        } else {
            // Mensaje enviado por un CLIENTE
            Client client = clientRepository.findById(requestDto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            messageEntity.setClient(client);
            messageEntity.setSenderType(SenderType.CLIENT);
        }

        // Guarda mensaje del ChatMessage en BD
        ChatMessageEntity savedMessage = chatRepository.save(messageEntity);
        // Settea DTO de respuesta
        ChatMessageDto responseDto = convertToDto(savedMessage);
        String destination = "/topic/qa/" + qa.getId();
        // (BROADCAST) Envia el mensaje en tiempo real a todos los clientes suscritos a ese DESTINATION
        messagingTemplate.convertAndSend(destination, responseDto);
        return responseDto;
    }

    // Recibe historial de mensajes tras la pregunta inicial
    public List<ChatMessageDto> getChatHistory(Long qaId) {
        // Obtiene historial del chat en base al ID del QA inicial
        List<ChatMessageEntity> messages = chatRepository.findByQaIdOrderByTimestampAsc(qaId);
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // FALTA PASARLO A DTO's
    public List<QA> getQAsByClient(Long clientId) {
        // Obtiene todas las preguntas hechas por el cliente
        return qaRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    // FALTA PASARLO A DTO's
    public List<QA> getUnrespondedQAs() {
        // Admin visualiza las preguntas no respondidas
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