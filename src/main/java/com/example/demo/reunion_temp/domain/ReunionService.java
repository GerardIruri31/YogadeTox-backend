package com.example.demo.reunion_temp.domain;

import com.example.demo.Events.EventsCalendarNotification.EventsNotificationEvent;
import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.config.JwtService;
import com.example.demo.config.GoogleTokenService;
import com.example.demo.exceptions.CalendarIntegrationException;
import com.example.demo.exceptions.EventAlreadyBookException;
import com.example.demo.exceptions.ResourceNotFoundException;

import com.example.demo.reunion_temp.dto.*;
import com.example.demo.reunion_temp.infraestructure.ReunionRepository;
import com.example.demo.config.GoogleCalendarOAuthService;
import com.google.api.services.calendar.model.Event;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReunionService {
    private static final Logger logger = LoggerFactory.getLogger(ReunionService.class);
    
    private final ReunionRepository reunionRepository;
    private final AdminRepository adminRepository;
    private final ClientRepository clientRepository;
    private final GoogleCalendarOAuthService googleCalendarOAuthService;
    private final GoogleTokenService googleTokenService;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    public CalendarEventDto createAvailableEvent(CreateEventRequestDto request) {
        String adminEmail = jwtService.getCurrentUserEmail();
        if (adminEmail == null) {
            throw new ResourceNotFoundException("No se pudo obtener el email del usuario autenticado");
        }
        
        Reunion reunion = new Reunion();
        reunion.setUrl("");
        reunion.setDescription(request.getDescription());
        reunion.setTag(request.getTag());
        reunion.setSessionDate(request.getStartTime());
        reunion.setHoraInicio(request.getStartTime());
        reunion.setHoraFin(request.getEndTime());
        
        Admin admin = adminRepository.findByEmail(adminEmail).orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado con email: " + adminEmail));
        reunion.setAdmin(admin);
        reunion.setCost(request.getCost());
        reunion.setIsCancelled(false);
        reunion.setClient(null);
        reunion.setGoogleEventId(null);

        reunionRepository.save(reunion);

        CalendarEventDto dto = new CalendarEventDto();
        dto.setEventId(reunion.getId());
        dto.setSummary(request.getSummary());
        dto.setDescription(request.getDescription());
        dto.setStartTime(request.getStartTime());
        dto.setEndTime(request.getEndTime());
        dto.setMeetingUrl("");
        dto.setAvailable(true);

        return dto;
    }

    // MÉTODO DE PRUEBA - SIN AUTENTICACIÓN
    public CalendarEventDto createAvailableEventTest(CreateEventRequestDto request, Long adminId) {
        Reunion reunion = new Reunion();
        reunion.setUrl("");
        reunion.setDescription(request.getDescription());
        reunion.setTag(request.getTag());
        reunion.setSessionDate(request.getStartTime());
        reunion.setHoraInicio(request.getStartTime());
        reunion.setHoraFin(request.getEndTime());
        
        // Obtener admin por email del JWT en lugar de adminId
        String adminEmail = jwtService.getCurrentUserEmail();
        if (adminEmail == null) {
            throw new ResourceNotFoundException("No se pudo obtener el email del usuario autenticado");
        }
        
        Admin admin = adminRepository.findByEmail(adminEmail).orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado con email: " + adminEmail));
        reunion.setAdmin(admin);
        reunion.setCost(request.getCost());
        reunion.setIsCancelled(false);
        reunion.setClient(null);
        reunion.setGoogleEventId(null);

        reunionRepository.save(reunion);

        CalendarEventDto dto = new CalendarEventDto();
        dto.setEventId(reunion.getId());
        dto.setSummary(request.getSummary());
        dto.setDescription(request.getDescription());
        dto.setStartTime(request.getStartTime());
        dto.setEndTime(request.getEndTime());
        dto.setMeetingUrl("");
        dto.setAvailable(true);

        return dto;
    }

    public List<CalendarEventDto> getAvailableEvents(){
        LocalDateTime now = LocalDateTime.now();
        List<Reunion> availableReunions = reunionRepository.findByClientIdIsNullAndSessionDateAfterAndIsCancelledFalse(now);
        return availableReunions.stream()
                .map(this::convertReunionToDto)
                .collect(Collectors.toList());
    }

    public BookEventResponseDto bookEvent(BookEventRequestDto request) {
        logger.info("=== BOOK EVENT DEBUG ===");
        logger.info("Request recibido: {}", request);
        logger.info("EventId del request: {}", request.getEventId());
        logger.info("Tipo de eventId: {}", request.getEventId() != null ? request.getEventId().getClass().getSimpleName() : "NULL");
        
        // Validar que el eventId no sea null
        if (request.getEventId() == null) {
            logger.error("❌ Event ID es NULL!");
            throw new IllegalArgumentException("Event ID no puede ser null");
        }
        
        logger.info("✅ Event ID válido: {}", request.getEventId());
        
        Long clientId = jwtService.getCurrentUserId();
        logger.info("Client ID del JWT: {}", clientId);
        
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        logger.info("✅ Cliente encontrado: {}", client.getEmail());
        
        logger.info("Buscando reunión con ID: {}", request.getEventId());
        Reunion reunion = reunionRepository.findById(request.getEventId()).orElseThrow(() -> new ResourceNotFoundException("Reunion no encontrada"));
        logger.info("✅ Reunión encontrada: {}", reunion.getId());
        if (reunion.getClient() != null) {
            throw new EventAlreadyBookException("Evento ya está reservado");
        }
        Event calendarEvent = null;
        String meetUrl = "";
        LocalDateTime endTime = reunion.getHoraFin();
        String eventDescription = reunion.getDescription();
        
        // Usar email fijo del organizador que tiene los tokens de Google Calendar
        String organizerEmail = "yogadetoxj@gmail.com";
        logger.info("Usando email fijo del organizador: {}", organizerEmail);
        
        // Verificar si el usuario tiene tokens de Google Calendar
        try {
            googleTokenService.getValidAccessToken(organizerEmail);
            logger.info("✅ Usuario {} tiene tokens válidos de Google Calendar", organizerEmail);
        } catch (Exception e) {
            logger.error("❌ Usuario {} no tiene tokens de Google Calendar: {}", organizerEmail, e.getMessage());
            throw new CalendarIntegrationException(
                "Para reservar eventos necesitas autorizar Google Calendar. " +
                "Por favor, visita: http://localhost:8080/oauth2/google/calendar/connect"
            );
        }
        
        try {
            // Crear evento CON Google Meet e invitación al cliente usando OAuth2
            logger.info("Intentando crear evento en Google Calendar con OAuth2 y Google Meet...");
            logger.info("Email del organizador: {}", organizerEmail);
            calendarEvent = googleCalendarOAuthService.createEventWithMeetAndInvite(
                    "Sesión de " + reunion.getTag(),
                    eventDescription + "\n\nCliente: " + client.getFirstName() + " " + client.getLastName() + " (" + client.getEmail() + ")",
                    reunion.getSessionDate(),
                    endTime,
                    client.getEmail(), // Enviar invitación al cliente
                    organizerEmail // Email del organizador desde JWT
            );
            
            // Extraer enlace de Google Meet (con manejo de errores)
            try {
                meetUrl = googleCalendarOAuthService.extractMeetUrl(calendarEvent);
                logger.info("✅ Google Meet URL extraída: {}", meetUrl);
            } catch (Exception e) {
                logger.warn("⚠️ No se pudo extraer Google Meet URL: {}", e.getMessage());
                meetUrl = "";
            }
            
            logger.info("✅ Evento creado exitosamente en Google Calendar: {}", calendarEvent.getId());
            logger.info("✅ URL del evento: {}", calendarEvent.getHtmlLink());
            logger.info("✅ Google Meet URL final: {}", meetUrl);
            
        } catch (IOException | GeneralSecurityException e) {
            logger.error("Error creando evento en Google Calendar: {}", e.getMessage());
            e.printStackTrace();
            // Lanzar excepción personalizada para manejo específico
            throw new CalendarIntegrationException("No se pudo crear el evento en Google Calendar: " + e.getMessage(), e);
        }

        if (calendarEvent != null) {
            reunion.setGoogleEventId(calendarEvent.getId());
            reunion.setUrl(meetUrl != null ? meetUrl : "");
        } else {
            reunion.setGoogleEventId(null);
            reunion.setUrl("");
        }
        reunion.setClient(client);
        reunion.setIsCancelled(false);

        Reunion savedReunion = reunionRepository.save(reunion);
        eventPublisher.publishEvent(new EventsNotificationEvent(this, savedReunion, client));
        return convertToBookEventResponseDto(savedReunion, client);
    }

    public void cancelReunion(Long reunionId) {
        Reunion reunion = reunionRepository.findById(reunionId).orElseThrow(() -> new ResourceNotFoundException("Reunion no encontrada"));

        if (reunion.getGoogleEventId() != null && !reunion.getGoogleEventId().isEmpty()) {
            try {
                // Usar email fijo del organizador que tiene los tokens de Google Calendar
                String organizerEmail = "yogadetoxj@gmail.com";
                logger.info("Usando email fijo del organizador para cancelar: {}", organizerEmail);
                
                googleCalendarOAuthService.deleteEvent(reunion.getGoogleEventId(), organizerEmail);
            } catch (IOException | GeneralSecurityException e) {
                logger.warn("No se pudo eliminar el evento del calendario: {}", e.getMessage());
            }
        }

        reunion.setIsCancelled(true);
        reunionRepository.save(reunion);
    }

    // Si soy cliente obtengo mis reus pendientes
    public List<MyReunionDto> getClientReunions() {
        Long clientId = jwtService.getCurrentUserId();
        List<Reunion> reunions = reunionRepository.findByClientId(clientId);
        return reunions.stream()
                .map(reunion -> modelMapper.map(reunion, MyReunionDto.class))
                .collect(Collectors.toList());
    }

    private CalendarEventDto convertReunionToDto(Reunion reunion) {
        LocalDateTime now = LocalDateTime.now();
        boolean isPast = reunion.getSessionDate() != null && reunion.getSessionDate().isBefore(now);
        boolean isAvailable = reunion.getClient() == null && !isPast;

        CalendarEventDto dto = new CalendarEventDto();
        dto.setEventId(reunion.getId());
        dto.setSummary("Sesión de " + (reunion.getTag()));
        dto.setDescription(reunion.getDescription());
        dto.setStartTime(reunion.getSessionDate());
        dto.setEndTime(reunion.getHoraFin());
        dto.setMeetingUrl(reunion.getUrl());
        dto.setAvailable(isAvailable);
        return dto;
    }

    private BookEventResponseDto convertToBookEventResponseDto(Reunion reunion, Client client) {
        BookEventResponseDto dto = new BookEventResponseDto();
        dto.setId(reunion.getId());
        dto.setUrl(reunion.getUrl());
        dto.setDescription(reunion.getDescription());
        dto.setTag(reunion.getTag());
        dto.setSesionDate(reunion.getSessionDate());
        dto.setHoraInicio(reunion.getHoraInicio());
        dto.setHoraFin(reunion.getHoraFin());
        dto.setCost(reunion.getCost());
        dto.setIsCancelled(reunion.getIsCancelled());
        dto.setClientName((client.getFirstName()) + " " +
                         (client.getLastName()));
        dto.setClientEmail(client.getEmail());
        dto.setMessage("Sesión reservada exitosamente");
        return dto;
    }

}