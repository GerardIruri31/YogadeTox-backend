package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final QASecurity qaSecurity; // <<-- inyectamos el servicio

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);
        StompCommand cmd = acc.getCommand();
        if (cmd == null) return message;

        // --- CONNECT: autenticar por JWT en headers nativos ---
        if (StompCommand.CONNECT.equals(cmd)) {
            try {
                String token = resolveTokenFromStompHeaders(acc);
                if (token == null) {
                    System.err.println("WebSocket: Falta token en CONNECT");
                    throw new AccessDeniedException("Falta token en CONNECT");
                }

                jwtService.validateToken(token); // lanza si inválido
                String email = jwtService.extractUsername(token);
                Long userId = jwtService.extractUserId(token);
                if (email == null || userId == null) {
                    System.err.println("WebSocket: Token sin subject/userId");
                    throw new AccessDeniedException("Token sin subject/userId");
                }

                var userDetails = userDetailsService.loadUserByUsername(email);

                // IMPORTANTÍSIMO: principal == userId como String, authorities del usuario
                var auth = new UsernamePasswordAuthenticationToken(
                        String.valueOf(userId), // principal (usaremos este nombre en /user/**)
                        null,
                        userDetails.getAuthorities()
                );
                acc.setUser(auth); // <<-- hace que Principal de esta sesión WS tenga name = userId
                System.out.println("WebSocket: Usuario autenticado correctamente: " + email + " (ID: " + userId + ")");
                return message;
            } catch (Exception e) {
                System.err.println("WebSocket: Error en autenticación: " + e.getMessage());
                throw new AccessDeniedException("Error en autenticación WebSocket: " + e.getMessage());
            }
        }

        // --- SUBSCRIBE: reglas por destino ---
        if (StompCommand.SUBSCRIBE.equals(cmd)) {
            Authentication auth = (Authentication) acc.getUser();
            String destination = acc.getDestination();
            
            if (destination == null) {
                System.err.println("WebSocket: Destino inválido");
                throw new AccessDeniedException("Destino inválido");
            }

            if (auth == null || !auth.isAuthenticated()) {
                System.err.println("WebSocket: Suscripción sin autenticación para destino: " + destination);
                // Permitir suscripciones a destinos de usuario y topic sin autenticación para debugging
                if (destination.startsWith("/user/") || destination.startsWith("/topic/")) {
                    System.out.println("WebSocket: Permitiendo suscripción a destino sin autenticación para debugging: " + destination);
                    return message;
                }
                throw new AccessDeniedException("Suscripción requiere autenticación");
            }

            System.out.println("WebSocket: Suscripción a " + destination + " por usuario " + auth.getName());
            
            // Permitir todas las suscripciones por ahora para debugging
            return message;
        }

        // --- SEND: permitir envíos a endpoints de app ---
        if (StompCommand.SEND.equals(cmd)) {
            Authentication auth = (Authentication) acc.getUser();
            String destination = acc.getDestination();
            
            if (destination == null) {
                System.err.println("WebSocket: Destino inválido en SEND");
                throw new AccessDeniedException("Destino inválido");
            }

            if (auth != null && auth.isAuthenticated()) {
                System.out.println("WebSocket: SEND a " + destination + " por usuario " + auth.getName());
                return message;
            } else {
                System.err.println("WebSocket: SEND a " + destination + " sin autenticación");
                throw new AccessDeniedException("SEND requiere autenticación");
            }
        }

        return message;
    }

    private String resolveTokenFromStompHeaders(StompHeaderAccessor acc) {
        List<String> authHeaders = Optional.ofNullable(acc.getNativeHeader("Authorization"))
                .orElseGet(() -> acc.getNativeHeader("authorization"));

        if (authHeaders == null || authHeaders.isEmpty()) return null;

        String raw = authHeaders.get(0);
        if (raw == null) return null;
        if (raw.startsWith("Bearer ")) return raw.substring(7);
        return raw;
    }

    private Long parseQaId(String idPart, String destination) {
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("QA id inválido en destino: " + destination);
        }
    }
}
