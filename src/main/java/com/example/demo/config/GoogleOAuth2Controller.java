package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/oauth2/google/calendar")
public class GoogleOAuth2Controller {

    @Autowired
    private GoogleTokenService tokenService;

    @Value("${google.calendar.client.id}")
    private String clientId;

    @Value("${google.calendar.secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect.uri}")
    private String redirectUri;

    @Value("${google.calendar.id}")
    private String calendarId;

    // 3. Iniciar flujo OAuth2 - CON LOGS DETALLADOS PARA DEBUGGING
    @GetMapping("/connect")
    public ResponseEntity<String> connectToGoogleCalendar() {
        try {
            String state = UUID.randomUUID().toString();
            
            System.out.println("=== CONFIGURACIÓN OAUTH2 DETALLADA ===");
            System.out.println("Client ID (GOOGLE_CALENDAR_CLIENT_ID): " + clientId);
            System.out.println("Client Secret (GOOGLE_CALENDAR_SECRET): " + (clientSecret != null ? clientSecret.substring(0, 10) + "..." : "NULL"));
            System.out.println("Redirect URI (GOOGLE_CALENDAR_REDIRECT_URI): " + redirectUri);
            System.out.println("Calendar ID (GOOGLE_CALENDAR_ID): " + calendarId);
            System.out.println("State: " + state);
            
            String authUrl = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "https://www.googleapis.com/auth/calendar")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .toUriString();

            System.out.println("🔗 URL de autorización completa: " + authUrl);
            System.out.println("📋 Redirect URI en la URL (SIN ENCODE): " + redirectUri);
            
            // Extraer el redirect_uri codificado de la URL para comparar
            String encodedRedirectUri = "";
            try {
                String[] parts = authUrl.split("redirect_uri=");
                if (parts.length > 1) {
                    String[] nextParts = parts[1].split("&");
                    encodedRedirectUri = nextParts[0];
                    System.out.println("📋 Redirect URI CODIFICADO en la URL: " + encodedRedirectUri);
                }
            } catch (Exception e) {
                System.out.println("⚠️ No se pudo extraer redirect_uri codificado");
            }
            
            return ResponseEntity.ok("=== CONFIGURACIÓN OAUTH2 DETALLADA ===\n" +
                "Client ID (GOOGLE_CALENDAR_CLIENT_ID): " + clientId + "\n" +
                "Client Secret (GOOGLE_CALENDAR_SECRET): " + (clientSecret != null ? clientSecret.substring(0, 10) + "..." : "NULL") + "\n" +
                "Redirect URI (GOOGLE_CALENDAR_REDIRECT_URI): " + redirectUri + "\n" +
                "Redirect URI (CODIFICADO): " + encodedRedirectUri + "\n" +
                "Calendar ID (GOOGLE_CALENDAR_ID): " + calendarId + "\n" +
                "State: " + state + "\n" +
                "\n" +
                "🔗 URL de autorización completa:\n" + authUrl + "\n" +
                "\n" +
                "📋 COPIA ESTA REDIRECT_URI EXACTA (SIN ENCODE):\n" +
                redirectUri + "\n" +
                "\n" +
                "📝 REGÍSTRALA EN GOOGLE CLOUD CONFIGURATION\n" +
                "1. Ve a Google Cloud Console\n" +
                "2. APIs & Services → Credentials\n" +
                "3. OAuth 2.0 Client IDs → (tu Web client 775...)\n" +
                "4. Edit → Authorized redirect URIs\n" +
                "5. Agrega EXACTAMENTE: " + redirectUri + "\n" +
                "6. Save");
                
        } catch (Exception e) {
            System.err.println("❌ Error iniciando OAuth2: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // 4. Callback después de autorización - VERSIÓN ROBUSTA
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        
        System.out.println("=== CALLBACK OAUTH2 RECIBIDO ===");
        System.out.println("Code: " + code);
        System.out.println("State: " + state);
        System.out.println("Error: " + error);
        System.out.println("Error Description: " + errorDescription);
        
        // 1. Verificar si hay error de Google
        if (error != null) {
            System.err.println("❌ Error de Google: " + error);
            System.err.println("❌ Descripción: " + errorDescription);
            return ResponseEntity.status(400).body("Error de Google: " + error + 
                (errorDescription != null ? " - " + errorDescription : ""));
        }
        
        // 2. Verificar que tenemos el code
        if (code == null || code.trim().isEmpty()) {
            System.err.println("❌ No se recibió el código de autorización");
            return ResponseEntity.status(400).body("Error: No se recibió el código de autorización");
        }
        
        try {
            System.out.println("✅ Código recibido: " + code);
            System.out.println("✅ State: " + state);
            System.out.println("✅ Redirect URI que se enviará al token endpoint: " + redirectUri);
            
            // 3. Intercambiar code por tokens y guardar en BD
            GoogleOAuthToken token = tokenService.exchangeCodeForTokens(code, calendarId);
            
            return ResponseEntity.ok("¡Conexión exitosa con Google Calendar!\n" +
                "Código: " + code + "\n" +
                "State: " + state + "\n" +
                "Redirect URI enviada: " + redirectUri + "\n" +
                "Organizador: " + token.getOrganizerEmail() + "\n" +
                "✅ Refresh token guardado");
                
        } catch (Exception e) {
            System.err.println("❌ Error en callback: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(502).body("Error intercambiando tokens: " + e.getMessage());
        }
    }
}