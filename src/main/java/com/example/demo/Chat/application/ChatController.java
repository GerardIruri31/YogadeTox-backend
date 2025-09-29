package com.example.demo.Chat.application;

import com.example.demo.Chat.domain.ChatService;
import com.example.demo.Chat.dto.ChatMessageDto;
import com.example.demo.Chat.dto.ChatRequestDto;
import com.example.demo.Chat.dto.ChatResponseDto;
import com.example.demo.Chat.dto.ConversationDto;
import com.example.demo.Chat.dto.QAPendingDto;
import com.example.demo.Chat.dto.ChatStatsDto;
import com.example.demo.qa.domain.QA;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;

    @GetMapping("/test")
    public String getWebSocketTestPage() {
        return "WebSocket";
    }

    @GetMapping("/client")
    public String getClientChatPage() {
        return "ClientChat";
    }

    @GetMapping("/admin")
    public String getAdminChatPage() {
        return "AdminChat";
    }

//    @PostMapping("/send")
//    @ResponseBody
//    public ResponseEntity<ChatMessageDto> sendMessage(@RequestBody ChatRequestDto request) {
//        return ResponseEntity.ok(chatService.saveMessage(request));
//    }

    @GetMapping("/history/{qaId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long qaId, Authentication auth) {
        // Validar que el usuario tenga acceso a esta conversación
        Long userId = Long.valueOf(auth.getName());
        if (!chatService.hasAccessToChat(qaId, userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(chatService.getChatHistory(qaId));
    }

    @GetMapping("/qa/client/{clientId}")
    @ResponseBody
    public ResponseEntity<List<QA>> getQAsByClient(@PathVariable Long clientId, Authentication auth) {
        // Solo el propio cliente o un admin puede ver sus conversaciones
        Long userId = Long.valueOf(auth.getName());
        if (!userId.equals(clientId) && !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(chatService.getQAsByClient(clientId));
    }

    @GetMapping("/qa/unresponded")
    @ResponseBody
    public ResponseEntity<List<QA>> getUnrespondedQAs() {
        return ResponseEntity.ok(chatService.getUnrespondedQAs());
    }

    // Endpoint adicional para obtener QAs no respondidas con información del cliente
    @GetMapping("/qa/unresponded-with-client")
    @ResponseBody
    public ResponseEntity<List<QAPendingDto>> getUnrespondedQAsWithClient() {
        return ResponseEntity.ok(chatService.getUnrespondedQAsWithClientInfo());
    }

    // ========== NUEVOS ENDPOINTS MEJORADOS ==========

    // Iniciar un nuevo chat (simplificado)
    @PreAuthorize("hasRole('FREE')")
    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<ChatResponseDto> startChat(@RequestBody ChatRequestDto request, Authentication auth) {
        try {
            Long clientId = Long.valueOf(auth.getName()); // Obtener del JWT
            return ResponseEntity.ok(chatService.startChat(clientId, request.getMessage()));
        } catch (Exception e) {
            System.err.println("Error en startChat: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Lista de conversaciones para Admin (como WhatsApp)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/conversations")
    @ResponseBody
    public ResponseEntity<List<ConversationDto>> getAdminConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getAdminConversations(page, size));
    }

    // Lista de conversaciones para Cliente
    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/client/conversations")
    @ResponseBody
    public ResponseEntity<List<ConversationDto>> getClientConversations(Authentication auth) {
        try {
            Long clientId = Long.valueOf(auth.getName());
            return ResponseEntity.ok(chatService.getClientConversations(clientId));
        } catch (Exception e) {
            System.err.println("Error en getClientConversations: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Enviar mensaje a un chat específico (REST) - Para clientes
    @PreAuthorize("hasRole('FREE')")
    @PostMapping("/send/{chatId}")
    @ResponseBody
    public ResponseEntity<ChatMessageDto> sendMessageToChat(
            @PathVariable Long chatId, 
            @RequestBody ChatRequestDto request, 
            Authentication auth) {
        Long senderId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(chatService.sendMessageToChat(chatId, senderId, request.getMessage()));
    }

    // Enviar mensaje a un chat específico (REST) - Para admins
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/send/{chatId}")
    @ResponseBody
    public ResponseEntity<ChatMessageDto> sendAdminMessageToChat(
            @PathVariable Long chatId, 
            @RequestBody ChatRequestDto request, 
            Authentication auth) {
        Long senderId = Long.valueOf(auth.getName());
        return ResponseEntity.ok(chatService.sendMessageToChat(chatId, senderId, request.getMessage()));
    }

    // Obtener información de una conversación específica
    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/conversation/{chatId}")
    @ResponseBody
    public ResponseEntity<ChatMessageDto> getConversation(@PathVariable Long chatId, Authentication auth) {
        // Validar acceso a la conversación
        Long userId = Long.valueOf(auth.getName());
        if (!chatService.hasAccessToChat(chatId, userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(chatService.getConversationById(chatId));
    }

    // Marcar conversación como leída
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/mark-read/{chatId}")
    @ResponseBody
    public ResponseEntity<String> markAsRead(@PathVariable Long chatId) {
        chatService.markConversationAsRead(chatId);
        return ResponseEntity.ok("Conversación marcada como leída");
    }

    // Obtener estadísticas del chat
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/stats")
    @ResponseBody
    public ResponseEntity<ChatStatsDto> getChatStats() {
        return ResponseEntity.ok(chatService.getChatStats());
    }

    // Buscar conversaciones
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/search")
    @ResponseBody
    public ResponseEntity<List<ConversationDto>> searchConversations(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.searchConversations(query, page, size));
    }

}