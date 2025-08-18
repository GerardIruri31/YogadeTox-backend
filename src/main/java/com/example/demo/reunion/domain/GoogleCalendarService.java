package com.example.demo.reunion.domain;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

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
                .fromStream(new ClassPathResource("credentials/green-buttress-469013-i4-1150b6fb799f.json").getInputStream())
                .createScoped(SCOPES);

        calendarService = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public Event createEventWithoutAttendees(String summary, String description, LocalDateTime startTime,
                                           LocalDateTime endTime) throws IOException {

        if (calendarService == null) {
            throw new IOException("Google Calendar Service no está disponible");
        }

        if (startTime == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser null");
        }

        Event event = new Event().setSummary(summary).setDescription(description);

        DateTime startDateTime = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);

        LocalDateTime endDateTime = endTime != null ? endTime : startTime.plusHours(1);
        DateTime endDateTimeObj = new DateTime(endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime end = new EventDateTime().setDateTime(endDateTimeObj).setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);

        Event createdEvent = calendarService.events().insert(calendarId, event).execute();
        return createdEvent;
    }



    public void deleteEvent(String eventId) throws IOException {
        calendarService.events().delete(calendarId, eventId).execute();
    }
}
