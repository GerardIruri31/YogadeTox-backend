package com.example.demo.reunion_temp.domain;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class GoogleCalendarService {
    private static final String APPLICATION_NAME = "YogaDetox Calendar Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    private Calendar calendarService;
    private String calendarId;

    @Value("${google.calendar.id:primary}")
    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    @PostConstruct
    public void initializeCalendarService() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredential credential = GoogleCredential
                .fromStream(new ClassPathResource("credentials/green-buttress-469013-i4-8b054cb00c19.json").getInputStream())
                .createScoped(SCOPES);
        calendarService = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public Event createEventWithoutAttendees(String summary, String description, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        System.out.println("=== CREANDO EVENTO EN GOOGLE CALENDAR ===");
        System.out.println("Summary: " + summary);
        System.out.println("Description: " + description);
        System.out.println("StartTime: " + startTime);
        System.out.println("EndTime: " + endTime);
        System.out.println("CalendarId: " + calendarId);
        
        if (calendarService == null) {
            throw new IOException("Google Calendar Service no está disponible");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser null");
        }
        
        // Listar calendarios disponibles para debug
        try {
            System.out.println("=== CALENDARIOS DISPONIBLES ===");
            CalendarList calendarList = calendarService.calendarList().list().execute();
            for (CalendarListEntry calendar : calendarList.getItems()) {
                System.out.println("ID: " + calendar.getId() + " | Summary: " + calendar.getSummary() + " | Primary: " + calendar.getPrimary());
            }
        } catch (Exception e) {
            System.out.println("Error listando calendarios: " + e.getMessage());
        }
        
        Event event = new Event().setSummary(summary).setDescription(description);
        DateTime startDateTime = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);

        LocalDateTime endDateTime = endTime != null ? endTime : startTime.plusHours(1);
        DateTime endDateTimeObj = new DateTime(endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime end = new EventDateTime().setDateTime(endDateTimeObj).setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);
        
        System.out.println("Evento configurado, enviando a Google Calendar...");
        Event createdEvent = calendarService.events().insert(calendarId, event).execute();
        System.out.println("✅ Evento creado exitosamente: " + createdEvent.getId());
        System.out.println("✅ URL del evento: " + createdEvent.getHtmlLink());
        
        return createdEvent;
    }

    public Event createEventWithMeet(String summary, String description, 
                                   LocalDateTime startTime, LocalDateTime endTime, 
                                   String clientEmail) throws IOException {
        if (calendarService == null) {
            throw new IOException("Google Calendar Service no está disponible");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser null");
        }
        
        Event event = new Event()
            .setSummary(summary)
            .setDescription(description);
        
        // Configurar fechas
        DateTime startDateTime = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime start = new EventDateTime()
            .setDateTime(startDateTime)
            .setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);
        
        LocalDateTime endDateTime = endTime != null ? endTime : startTime.plusHours(1);
        DateTime endDateTimeObj = new DateTime(endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime end = new EventDateTime()
            .setDateTime(endDateTimeObj)
            .setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);
        
        // COMENTADO: Google Meet puede requerir permisos especiales
        // AGREGAR GOOGLE MEET
        // ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey()
        //     .setType("meet");
        // 
        // CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest()
        //     .setRequestId(UUID.randomUUID().toString())
        //     .setConferenceSolutionKey(conferenceSolutionKey);
        // 
        // ConferenceData conferenceData = new ConferenceData()
        //     .setCreateRequest(createConferenceRequest);
        // 
        // event.setConferenceData(conferenceData);
        
        // COMENTADO: Service Accounts no pueden invitar usuarios sin Domain-Wide Delegation
        // Agregar invitado
        // if (clientEmail != null && !clientEmail.isEmpty()) {
        //     EventAttendee attendee = new EventAttendee()
        //         .setEmail(clientEmail)
        //         .setResponseStatus("accepted");
        //     event.setAttendees(Collections.singletonList(attendee));
        // }
        
        // Configurar recordatorios
        EventReminder[] reminders = {
            new EventReminder().setMethod("email").setMinutes(24 * 60), // 1 día antes
            new EventReminder().setMethod("popup").setMinutes(30)       // 30 min antes
        };
        event.setReminders(new Event.Reminders()
            .setUseDefault(false)
            .setOverrides(Arrays.asList(reminders)));
        
        // Crear evento simple (sin conferencia por ahora)
        Event createdEvent = calendarService.events()
            .insert(calendarId, event)
            .execute();
        
        return createdEvent;
    }
    

    // METODO ALTERNATIVO: Crear evento simple SIN Google Meet (para Service Accounts)
    public Event createSimpleEvent(String summary, String description, 
                                 LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        if (calendarService == null) {
            throw new IOException("Google Calendar Service no está disponible");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser null");
        }
        
        Event event = new Event()
            .setSummary(summary)
            .setDescription(description);
        
        // Configurar fechas
        DateTime startDateTime = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime start = new EventDateTime()
            .setDateTime(startDateTime)
            .setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);
        
        LocalDateTime endDateTime = endTime != null ? endTime : startTime.plusHours(1);
        DateTime endDateTimeObj = new DateTime(endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime end = new EventDateTime()
            .setDateTime(endDateTimeObj)
            .setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);
        
        // Configurar recordatorios
        EventReminder[] reminders = {
            new EventReminder().setMethod("email").setMinutes(24 * 60), // 1 día antes
            new EventReminder().setMethod("popup").setMinutes(30)       // 30 min antes
        };
        event.setReminders(new Event.Reminders()
            .setUseDefault(false)
            .setOverrides(Arrays.asList(reminders)));
        
        // Crear evento simple (sin conferencia)
        Event createdEvent = calendarService.events()
            .insert(calendarId, event)
            .execute();
        
        return createdEvent;
    }

    public Event createEventWithMeetAndInvite(
            String summary,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String clientEmail
    ) throws IOException {

        if (calendarService == null) throw new IOException("Google Calendar Service no está disponible");
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

        // Attendees (cliente)
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

        // Insert con Meet + envío de invitación
        Calendar.Events.Insert insert = calendarService.events().insert(calendarId, event);
        insert.setConferenceDataVersion(1); // ¡Obligatorio para que se genere el Meet!
        insert.setSendUpdates("all");       // envía email al cliente

        Event created = insert.execute();

        // Útil para tu log
        System.out.println("✅ Evento creado: " + created.getId());
        System.out.println("🔗 HTML: " + created.getHtmlLink());
        System.out.println("🎥 Meet: " + created.getHangoutLink());

        return created;
    }

    public Event addMeetAndInviteToExisting(String eventId, String clientEmail) throws IOException {
        if (calendarService == null) throw new IOException("Google Calendar Service no está disponible");

        // Traer el evento actual para no perder otros campos/attendees
        Event current = calendarService.events().get(calendarId, eventId).execute();

        // Merge de asistentes (si ya hay otros, se conservan)
        List<EventAttendee> attendees = current.getAttendees() != null
                ? current.getAttendees()
                : new java.util.ArrayList<>();
        if (clientEmail != null && !clientEmail.isBlank()
                && attendees.stream().noneMatch(a -> clientEmail.equalsIgnoreCase(a.getEmail()))) {
            attendees.add(new EventAttendee().setEmail(clientEmail));
        }
        current.setAttendees(attendees);

        // Solicitar Meet
        String requestId = UUID.randomUUID().toString();
        ConferenceSolutionKey key = new ConferenceSolutionKey().setType("hangoutsMeet");
        CreateConferenceRequest cReq = new CreateConferenceRequest()
                .setRequestId(requestId)
                .setConferenceSolutionKey(key);
        current.setConferenceData(new ConferenceData().setCreateRequest(cReq));

        // Patch con Meet + envío de actualización al cliente
        Calendar.Events.Patch patch = calendarService.events().patch(calendarId, eventId, current);
        patch.setConferenceDataVersion(1);
        patch.setSendUpdates("all");

        Event updated = patch.execute();

        System.out.println("✅ Evento actualizado (Meet añadido): " + updated.getId());
        System.out.println("🎥 Meet: " + updated.getHangoutLink());

        return updated;
    }

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

    public void deleteEvent(String eventId) throws IOException {
        calendarService.events().delete(calendarId, eventId).execute();
    }
}