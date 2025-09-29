package com.example.demo.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class GoogleCalendarOAuthService {

    private static final String APPLICATION_NAME = "YogaDetox Calendar Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    @Autowired
    private GoogleTokenService tokenService;

    @Value("${google.calendar.id}")
    private String calendarId;

    // 6. Crear cliente Calendar con OAuth de usuario
    public Calendar createCalendarClient(String organizerEmail) throws IOException, GeneralSecurityException {
        String accessToken = tokenService.getValidAccessToken(organizerEmail);
        
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        // Crear credential con access token
        Credential credential = new GoogleCredential()
            .setAccessToken(accessToken);
        
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    // 7. Crear evento con Meet + invitados usando OAuth
    public Event createEventWithMeetAndInvite(
            String summary,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String clientEmail,
            String organizerEmail
    ) throws IOException, GeneralSecurityException {

        Calendar calendarService = createCalendarClient(organizerEmail);
        
        if (startTime == null) throw new IllegalArgumentException("La fecha de inicio no puede ser null");

        // Zona horaria (recomendado fijar la de Lima para evitar sorpresas)
        final String tz = "America/Lima";

        Event event = new Event()
                .setSummary(summary)
                .setDescription(description)
                .setGuestsCanInviteOthers(false)
                .setGuestsCanSeeOtherGuests(false)
                .setVisibility("private");

        // Fechas
        DateTime startDt = new DateTime(startTime.atZone(ZoneId.of(tz)).toInstant().toEpochMilli());
        DateTime endDt   = new DateTime((endTime != null ? endTime : startTime.plusHours(1))
                                        .atZone(ZoneId.of(tz)).toInstant().toEpochMilli());
        event.setStart(new EventDateTime().setDateTime(startDt).setTimeZone(tz));
        event.setEnd  (new EventDateTime().setDateTime(endDt)  .setTimeZone(tz));

        // Attendees (cliente) - AHORA SÍ FUNCIONA CON OAUTH
        if (clientEmail != null && !clientEmail.isBlank()) {
            event.setAttendees(Collections.singletonList(new EventAttendee().setEmail(clientEmail)));
        }

        // Solicitar Google Meet (idempotente con requestId estable por reserva)
        String requestId = UUID.randomUUID().toString();
        ConferenceSolutionKey key = new ConferenceSolutionKey().setType("hangoutsMeet");
        CreateConferenceRequest cReq = new CreateConferenceRequest()
                .setRequestId(requestId)
                .setConferenceSolutionKey(key);
        event.setConferenceData(new ConferenceData().setCreateRequest(cReq));

        // Insert con Meet + envío de invitación - AHORA SÍ FUNCIONA
        Calendar.Events.Insert insert = calendarService.events().insert(calendarId, event);
        insert.setConferenceDataVersion(1); // ¡Obligatorio para que se genere el Meet!
        insert.setSendUpdates("all");       // envía email al cliente

        Event created = insert.execute();

        // Útil para tu log
        System.out.println("✅ Evento creado con OAuth: " + created.getId());
        System.out.println("🔗 HTML: " + created.getHtmlLink());
        System.out.println("🎥 Meet: " + created.getHangoutLink());

        return created;
    }

    // Extraer Google Meet URL
    public String extractMeetUrl(Event event) {
        if (event == null) {
            System.out.println("⚠️ Event es null en extractMeetUrl");
            return "";
        }
        
        try {
            // Intentar obtener hangoutLink primero (más directo)
            if (event.getHangoutLink() != null && !event.getHangoutLink().isEmpty()) {
                System.out.println("✅ Encontrado hangoutLink: " + event.getHangoutLink());
                return event.getHangoutLink();
            }
            
            // Si no hay hangoutLink, buscar en conferenceData
            if (event.getConferenceData() != null && 
                event.getConferenceData().getEntryPoints() != null) {
                
                System.out.println("🔍 Buscando en conferenceData...");
                for (EntryPoint entryPoint : event.getConferenceData().getEntryPoints()) {
                    System.out.println("EntryPoint type: " + entryPoint.getEntryPointType() + ", URI: " + entryPoint.getUri());
                    if ("video".equals(entryPoint.getEntryPointType())) {
                        System.out.println("✅ Encontrado video entryPoint: " + entryPoint.getUri());
                        return entryPoint.getUri();
                    }
                }
            }
            
            System.out.println("⚠️ No se encontró Google Meet URL");
            return "";
            
        } catch (Exception e) {
            System.out.println("❌ Error extrayendo Meet URL: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    // Eliminar evento del calendario
    public void deleteEvent(String eventId) throws IOException, GeneralSecurityException {
        deleteEvent(eventId, "yogadetoxj@gmail.com"); // Fallback por defecto
    }
    
    // Eliminar evento del calendario con email del organizador
    public void deleteEvent(String eventId, String organizerEmail) throws IOException, GeneralSecurityException {
        if (eventId == null || eventId.isEmpty()) {
            System.out.println("⚠️ Event ID es null o vacío");
            return;
        }
        
        if (organizerEmail == null || organizerEmail.isEmpty()) {
            System.out.println("⚠️ Organizer email es null o vacío, usando fallback");
            organizerEmail = "yogadetoxj@gmail.com";
        }
        
        Calendar calendarService = createCalendarClient(organizerEmail);
        
        try {
            calendarService.events().delete(calendarId, eventId).execute();
            System.out.println("✅ Evento eliminado exitosamente: " + eventId);
        } catch (Exception e) {
            System.out.println("❌ Error eliminando evento: " + e.getMessage());
            throw e;
        }
    }
}
