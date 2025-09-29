package com.example.demo.Chat.domain;

import com.example.demo.Chat.dto.*;
import com.example.demo.Chat.infrastructure.ChatMessageRepository;
import com.example.demo.admin.domain.Admin;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.qa.domain.QA;
import com.example.demo.qa.infraestructure.QARepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatRepository;
    private final QARepository qaRepository;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher events;

    @Transactional
    public ChatMessageDto saveMessage(ChatRequestDto requestDto) {
        if (requestDto.getMessage() == null || requestDto.getMessage().isBlank()) {
            throw new IllegalArgumentException("Mensaje vacío");
        }

        QA qa;
        if (requestDto.getQaId() == null) {
            // Primera pregunta del cliente
            if (requestDto.getClientId() == null) {
                throw new IllegalArgumentException("clientId requerido para primera pregunta");
            }
            Client client = clientRepository.findById(requestDto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

            qa = new QA();
            qa.setMessage(requestDto.getMessage());
            qa.setClient(client);
            qa.setCreatedAt(ZonedDateTime.now());
            qa.setResponded(false); // cliente pregunta ⇒ pendiente para admin
            qa = qaRepository.save(qa);
        } else {
            qa = qaRepository.findById(requestDto.getQaId())
                    .orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));
        }

        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(requestDto.getMessage());
        messageEntity.setTimestamp(ZonedDateTime.now());
        messageEntity.setQa(qa);

        boolean pendingForAdmin;

        if (requestDto.getAdminId() != null) {
            // Mensaje de ADMIN
            Admin admin = adminRepository.findById(requestDto.getAdminId())
                    .orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));
            messageEntity.setAdmin(admin);
            messageEntity.setSenderType(SenderType.ADMIN);

            qa.setResponded(true); // admin responde ⇒ deja de estar pendiente
            pendingForAdmin = false;
            qaRepository.save(qa);
        } else if (requestDto.getClientId() != null) {
            // Mensaje de CLIENTE
            Client client = clientRepository.findById(requestDto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            messageEntity.setClient(client);
            messageEntity.setSenderType(SenderType.CLIENT);

            qa.setResponded(false); // cliente escribe de nuevo ⇒ vuelve a pendiente
            pendingForAdmin = true;
            qaRepository.save(qa);
        } else {
            throw new IllegalArgumentException("Debe venir adminId o clientId");
        }

        ChatMessageEntity savedMessage = chatRepository.save(messageEntity);
        ChatMessageDto dto = convertToDto(savedMessage);

        // Publica evento (el listener WS enviará AFTER_COMMIT)
        events.publishEvent(new ChatMessageCreatedEvent(
                qa.getId(),
                dto,
                qa.getClient().getId(),
                pendingForAdmin
        ));

        return dto;
    }

    public List<ChatMessageDto> getChatHistory(Long qaId) {
        // Traer solo los últimos 50 mensajes para evitar problemas de scroll con conversaciones muy largas
        List<ChatMessageEntity> messages = chatRepository.findTop50ByQaIdOrderByTimestampDesc(qaId);
        // Invertir la lista para mostrar los más antiguos primero y los más recientes al final
        Collections.reverse(messages);
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

    public List<QAPendingDto> getUnrespondedQAsWithClientInfo() {
        return qaRepository.findByIsRespondedOrderByCreatedAtDesc(false)
                .stream()
                .map(qa -> new QAPendingDto(
                        qa.getId(),
                        qa.getMessage(),
                        qa.getClient().getFirstName() + " " + qa.getClient().getLastName(),
                        qa.getClient().getId()
                ))
                .collect(Collectors.toList());
    }


    private ChatMessageDto convertToDto(ChatMessageEntity entity) {
        ChatMessageDto dto = modelMapper.map(entity, ChatMessageDto.class);
        dto.setQaId(entity.getQa().getId());
        dto.setIsFirstMessage(entity.getIsFirstMessage());
        dto.setMessageType(entity.getMessageType());
        dto.setConversationTitle(entity.getQa().getTitle());
        
        if (entity.getSenderType() == SenderType.CLIENT && entity.getClient() != null) {
            dto.setSenderId(entity.getClient().getId());
            dto.setSenderName(entity.getClient().getFirstName() + " " + entity.getClient().getLastName());
        } else if (entity.getSenderType() == SenderType.ADMIN && entity.getAdmin() != null) {
            dto.setSenderId(entity.getAdmin().getId());
            dto.setSenderName(entity.getAdmin().getFirstName() + " " + entity.getAdmin().getLastName());
        }
        return dto;
    }

    @Transactional
    public ChatMessageDto saveMessageFromWs(Long qaId, Long senderUserId, String content) {
        var qa = qaRepository.findById(qaId)
                .orElseThrow(() -> new ResourceNotFoundException("QA no encontrada"));

        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Mensaje vacío");

        // ¿El sender es admin o client? (herencia JOINED => misma PK que User)
        Optional<Admin> adminOpt = adminRepository.findById(senderUserId);
        Optional<Client> clientOpt = clientRepository.findById(senderUserId);

        if (adminOpt.isEmpty() && clientOpt.isEmpty())
            throw new AccessDeniedException("Usuario no válido");

        ChatMessageEntity msg = new ChatMessageEntity();
        msg.setQa(qa);
        msg.setContent(content);
        msg.setTimestamp(ZonedDateTime.now());

        boolean pendingForAdmin;

        if (adminOpt.isPresent()) {
            msg.setSenderType(SenderType.ADMIN);
            msg.setAdmin(adminOpt.get());
            qa.setResponded(true);
            pendingForAdmin = false;
        } else {
            msg.setSenderType(SenderType.CLIENT);
            msg.setClient(clientOpt.get());
            qa.setResponded(false);
            pendingForAdmin = true;
        }
        qaRepository.save(qa);

        var saved = chatRepository.save(msg);
        var dto = convertToDto(saved);

        // Si ya usas tu WsNotifier con AFTER_COMMIT, puedes publicar evento.
        // Aquí hacemos el return; el WS controller decide si también emite.
        events.publishEvent(new ChatMessageCreatedEvent(
                qa.getId(),
                dto,
                qa.getClient().getId(),
                pendingForAdmin
        ));

        return dto;
    }

    private ConversationDto convertToConversationDto(QA qa) {
        ConversationDto dto = new ConversationDto();
        dto.setChatId(qa.getId());
        dto.setClientId(qa.getClient().getId());
        dto.setClientName(qa.getClient().getFirstName() + " " + qa.getClient().getLastName());
        dto.setIsActive(qa.getIsActive());
        dto.setLastMessageTime(qa.getLastMessageTime());
        dto.setConversationTitle(qa.getTitle());
        
        // Obtener el último mensaje
        List<ChatMessageEntity> messages = chatRepository.findByQaIdOrderByTimestampDesc(qa.getId());
        if (!messages.isEmpty()) {
            dto.setLastMessage(messages.get(0).getContent());
        }
        
        // Contar mensajes no leídos (simplificado - en una implementación real necesitarías un campo de estado)
        dto.setUnreadCount(0); // Por ahora siempre 0, se puede mejorar después
        
        return dto;
    }

    public boolean hasAccessToChat(Long chatId, Long userId) {
        try {
            QA qa = qaRepository.findById(chatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chat no encontrado"));
            
            // Verificar si es el cliente propietario del chat
            if (qa.getClient().getId().equals(userId)) {
                return true;
            }
            
            // Verificar si es un admin
            Optional<Admin> adminOpt = adminRepository.findById(userId);
            return adminOpt.isPresent();
            
        } catch (Exception e) {
            return false;
        }
    }

    // Nuevos métodos para el sistema de chat mejorado

    @Transactional
    public ChatResponseDto startChat(Long clientId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Mensaje vacío");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        // Buscar conversación activa existente
        Optional<QA> existingQa = qaRepository.findByClientIdAndIsActiveTrueOrderByLastMessageTimeDesc(clientId)
                .stream()
                .findFirst();

        QA qa;
        boolean isNewChat = false;

        if (existingQa.isPresent()) {
            // Reutilizar conversación existente
            qa = existingQa.get();
            qa.setLastMessageTime(ZonedDateTime.now());
            qa.setResponded(false);
            qa.setMessageCount(qa.getMessageCount() + 1);
            qa = qaRepository.save(qa);
        } else {
            // Crear nueva conversación
            qa = new QA();
            qa.setMessage(message);
            qa.setClient(client);
            qa.setCreatedAt(ZonedDateTime.now());
            qa.setLastMessageTime(ZonedDateTime.now());
            qa.setResponded(false);
            qa.setIsActive(true);
            qa.setMessageCount(1);
            qa.setTitle(message.length() > 50 ? message.substring(0, 50) + "..." : message);
            qa = qaRepository.save(qa);
            isNewChat = true;
        }

        // Crear primer mensaje
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(message);
        messageEntity.setTimestamp(ZonedDateTime.now());
        messageEntity.setQa(qa);
        messageEntity.setClient(client);
        messageEntity.setSenderType(SenderType.CLIENT);
        messageEntity.setIsFirstMessage(true);
        messageEntity.setMessageType("TEXT");

        chatRepository.save(messageEntity);

        // Publicar evento
        events.publishEvent(new ChatMessageCreatedEvent(
                qa.getId(),
                convertToDto(messageEntity),
                client.getId(),
                true
        ));

        // Crear respuesta
        ChatResponseDto response = new ChatResponseDto();
        response.setChatId(qa.getId());
        response.setMessage(isNewChat ? "Chat iniciado correctamente" : "Mensaje agregado a conversación existente");
        response.setIsNewChat(isNewChat);
        response.setConversationTitle(qa.getTitle());

        return response;
    }

    public List<ConversationDto> getAdminConversations() {
        List<QA> activeQAs = qaRepository.findByIsActiveTrueOrderByLastMessageTimeDesc();
        
        return activeQAs.stream()
                .map(this::convertToConversationDto)
                .collect(Collectors.toList());
    }

    public List<ConversationDto> getAdminConversations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastMessageTime").descending());
        Page<QA> activeQAs = qaRepository.findByIsActiveTrue(pageable);
        
        return activeQAs.getContent().stream()
                .map(this::convertToConversationDto)
                .collect(Collectors.toList());
    }

    public List<ConversationDto> getClientConversations(Long clientId) {
        List<QA> clientQAs = qaRepository.findByClientIdAndIsActiveTrueOrderByLastMessageTimeDesc(clientId);
        
        return clientQAs.stream()
                .map(this::convertToConversationDto)
                .collect(Collectors.toList());
    }

    public ChatMessageDto getConversationById(Long chatId) {
        QA qa = qaRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversación no encontrada"));

        List<ChatMessageEntity> messages = chatRepository.findByQaIdOrderByTimestampAsc(chatId);
        
        // Retornamos solo la información básica del chat, el historial se obtiene por separado
        ChatMessageDto dto = new ChatMessageDto();
        dto.setQaId(qa.getId());
        dto.setConversationTitle(qa.getTitle());
        
        return dto;
    }

    @Transactional
    public ChatMessageDto sendMessageToChat(Long chatId, Long senderId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Mensaje vacío");
        }

        QA qa = qaRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat no encontrado"));

        // Determinar si es admin o cliente
        Optional<Admin> adminOpt = adminRepository.findById(senderId);
        Optional<Client> clientOpt = clientRepository.findById(senderId);

        if (adminOpt.isEmpty() && clientOpt.isEmpty()) {
            throw new AccessDeniedException("Usuario no válido");
        }

        // Crear mensaje
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(message);
        messageEntity.setTimestamp(ZonedDateTime.now());
        messageEntity.setQa(qa);
        messageEntity.setIsFirstMessage(false);
        messageEntity.setMessageType("TEXT");

        boolean pendingForAdmin;

        if (adminOpt.isPresent()) {
            messageEntity.setAdmin(adminOpt.get());
            messageEntity.setSenderType(SenderType.ADMIN);
            qa.setResponded(true);
            pendingForAdmin = false;
        } else {
            messageEntity.setClient(clientOpt.get());
            messageEntity.setSenderType(SenderType.CLIENT);
            qa.setResponded(false);
            pendingForAdmin = true;
        }

        // Actualizar QA
        qa.setLastMessageTime(ZonedDateTime.now());
        qa.setMessageCount(qa.getMessageCount() + 1);
        qaRepository.save(qa);

        ChatMessageEntity savedMessage = chatRepository.save(messageEntity);
        ChatMessageDto dto = convertToDto(savedMessage);

        // Publicar evento
        events.publishEvent(new ChatMessageCreatedEvent(
                qa.getId(),
                dto,
                qa.getClient().getId(),
                pendingForAdmin
        ));

        return dto;
    }


    @Transactional
    public ChatMessageDto sendMessageUnified(Long chatId, Long senderId, String message, String messageType) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Mensaje vacío");
        }

        QA qa = qaRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat no encontrado"));

        // Validar acceso al chat
        if (!hasAccessToChat(chatId, senderId)) {
            throw new AccessDeniedException("No tienes acceso a este chat");
        }

        // Determinar si es admin o cliente
        Optional<Admin> adminOpt = adminRepository.findById(senderId);
        Optional<Client> clientOpt = clientRepository.findById(senderId);

        if (adminOpt.isEmpty() && clientOpt.isEmpty()) {
            throw new AccessDeniedException("Usuario no válido");
        }

        // Crear mensaje
        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(message);
        messageEntity.setTimestamp(ZonedDateTime.now());
        messageEntity.setQa(qa);
        messageEntity.setIsFirstMessage(false);
        messageEntity.setMessageType(messageType != null ? messageType : "TEXT");

        boolean pendingForAdmin;

        if (adminOpt.isPresent()) {
            messageEntity.setAdmin(adminOpt.get());
            messageEntity.setSenderType(SenderType.ADMIN);
            qa.setResponded(true);
            pendingForAdmin = false;
        } else {
            messageEntity.setClient(clientOpt.get());
            messageEntity.setSenderType(SenderType.CLIENT);
            qa.setResponded(false);
            pendingForAdmin = true;
        }

        // Actualizar QA
        qa.setLastMessageTime(ZonedDateTime.now());
        qa.setMessageCount(qa.getMessageCount() + 1);
        qaRepository.save(qa);

        ChatMessageEntity savedMessage = chatRepository.save(messageEntity);
        ChatMessageDto dto = convertToDto(savedMessage);

        // Publicar evento
        events.publishEvent(new ChatMessageCreatedEvent(
                qa.getId(),
                dto,
                qa.getClient().getId(),
                pendingForAdmin
        ));

        return dto;
    }

    @Transactional
    public void markConversationAsRead(Long chatId) {
        QA qa = qaRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat no encontrado"));
        
        qa.setResponded(true);
        qaRepository.save(qa);
    }

    public ChatStatsDto getChatStats() {
        ChatStatsDto stats = new ChatStatsDto();
        
        // Contar conversaciones
        stats.setTotalConversations(qaRepository.count());
        stats.setActiveConversations(qaRepository.countByIsActiveTrue());
        stats.setPendingConversations(qaRepository.countByIsActiveTrueAndIsRespondedFalse());
        
        // Contar mensajes
        stats.setTotalMessages(chatRepository.count());
        
        // Mensajes de hoy
        ZonedDateTime startOfDay = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0);
        stats.setMessagesToday(chatRepository.countByTimestampAfter(startOfDay));
        
        // Contar clientes únicos
        stats.setTotalClients(clientRepository.count());
        stats.setActiveClients(qaRepository.countDistinctClientIdByIsActiveTrue());
        
        // Tiempo promedio de respuesta (simplificado)
        stats.setAverageResponseTime(0.0); // Se puede implementar después
        
        return stats;
    }

    public List<ConversationDto> searchConversations(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastMessageTime").descending());
        
        // Buscar por nombre del cliente o contenido del mensaje
        List<QA> results = qaRepository.findByClientFirstNameContainingIgnoreCaseOrClientLastNameContainingIgnoreCaseOrTitleContainingIgnoreCase(
                query, query, query, pageable);
        
        return results.stream()
                .map(this::convertToConversationDto)
                .collect(Collectors.toList());
    }
}
