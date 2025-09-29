package com.example.demo.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

@Service
public class GoogleTokenService {

    @Autowired
    private GoogleOAuthTokenRepository tokenRepository;

    @Value("${google.calendar.client.id}")
    private String clientId;

    @Value("${google.calendar.secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 5.1. Intercambiar code por tokens - VERSIÓN ROBUSTA
    public GoogleOAuthToken exchangeCodeForTokens(String code, String organizerEmail) {
        try {
            String tokenUrl = "https://oauth2.googleapis.com/token";
            
            System.out.println("=== INTERCAMBIO DE TOKENS ===");
            System.out.println("Token URL: " + tokenUrl);
            System.out.println("Redirect URI (GOOGLE_CALENDAR_REDIRECT_URI): " + redirectUri);
            System.out.println("Client ID (GOOGLE_CALENDAR_CLIENT_ID): " + clientId);
            System.out.println("Client Secret (GOOGLE_CALENDAR_SECRET): " + (clientSecret != null ? clientSecret.substring(0, 10) + "..." : "NULL"));
            System.out.println("Organizer Email: " + organizerEmail);
            System.out.println("📋 VERIFICAR: Esta redirect_uri debe ser EXACTAMENTE la misma que registraste en Google Cloud");
            
            // Preparar el request body
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("code", code);
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("redirect_uri", redirectUri);
            requestBody.add("grant_type", "authorization_code");
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            // Hacer la petición
            System.out.println("🔄 Enviando petición al token endpoint...");
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            
            System.out.println("📊 Status Code: " + response.getStatusCode());
            System.out.println("📄 Response Body: " + response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                String accessToken = jsonResponse.get("access_token").asText();
                String refreshToken = jsonResponse.get("refresh_token") != null ? 
                    jsonResponse.get("refresh_token").asText() : null;
                int expiresIn = jsonResponse.get("expires_in").asInt();
                String scope = jsonResponse.get("scope").asText();
                String tokenType = jsonResponse.get("token_type").asText();
                
                // Verificar que tenemos refresh token
                if (refreshToken == null || refreshToken.trim().isEmpty()) {
                    throw new RuntimeException("Falta refresh_token. Repetir consentimiento con prompt=consent o revocar acceso y reintentar.");
                }
                
                LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
                
                // Guardar o actualizar en BD
                GoogleOAuthToken token = tokenRepository.findByOrganizerEmail(organizerEmail)
                    .orElse(new GoogleOAuthToken());
                
                token.setOrganizerEmail(organizerEmail);
                token.setAccessToken(accessToken);
                token.setRefreshToken(refreshToken);
                token.setExpiresAt(expiresAt);
                token.setScope(scope);
                token.setTokenType(tokenType);
                
                GoogleOAuthToken savedToken = tokenRepository.save(token);
                
                System.out.println("✅ Tokens guardados para: " + organizerEmail);
                System.out.println("🎫 Access Token: " + accessToken.substring(0, 20) + "...");
                System.out.println("🔄 Refresh Token: " + refreshToken.substring(0, 20) + "...");
                System.out.println("⏰ Expira en: " + expiresAt);
                
                return savedToken;
                
            } else {
                System.err.println("❌ Error del token endpoint: " + response.getStatusCode());
                System.err.println("❌ Response body: " + response.getBody());
                throw new RuntimeException("Error del token endpoint: " + response.getStatusCode() + " - " + response.getBody());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en exchangeCodeForTokens: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error intercambiando tokens: " + e.getMessage(), e);
        }
    }

    // 5.2. Obtener access token válido (renovación automática)
    public String getValidAccessToken(String organizerEmail) {
        try {
            Optional<GoogleOAuthToken> tokenOpt = tokenRepository.findByOrganizerEmail(organizerEmail);
            
            if (tokenOpt.isEmpty()) {
                throw new RuntimeException("No hay tokens guardados para: " + organizerEmail);
            }
            
            GoogleOAuthToken token = tokenOpt.get();
            
            // Verificar si el token está por expirar (5 minutos de margen)
            if (token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
                System.out.println("🔄 Token expirado, renovando...");
                return refreshAccessToken(token);
            }
            
            System.out.println("✅ Token válido para: " + organizerEmail);
            return token.getAccessToken();
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo access token: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error obteniendo access token: " + e.getMessage(), e);
        }
    }

    // Renovar access token usando refresh token
    private String refreshAccessToken(GoogleOAuthToken token) {
        try {
            String tokenUrl = "https://oauth2.googleapis.com/token";
            
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("grant_type", "refresh_token");
            requestBody.add("refresh_token", token.getRefreshToken());
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                String newAccessToken = jsonResponse.get("access_token").asText();
                int expiresIn = jsonResponse.get("expires_in").asInt();
                
                LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(expiresIn);
                
                // Actualizar en BD
                token.setAccessToken(newAccessToken);
                token.setExpiresAt(newExpiresAt);
                tokenRepository.save(token);
                
                System.out.println("✅ Token renovado exitosamente");
                System.out.println("🎫 Nuevo Access Token: " + newAccessToken.substring(0, 20) + "...");
                System.out.println("⏰ Nuevo expira en: " + newExpiresAt);
                
                return newAccessToken;
                
            } else {
                throw new RuntimeException("Error renovando token: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error renovando access token: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error renovando access token: " + e.getMessage(), e);
        }
    }
}
