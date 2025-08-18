package com.example.demo.reunion.domain;

import com.example.demo.Events.EmailService;
import com.example.demo.Events.EventsCalendarNotification.EventsNotificationEvent;
import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.client.domain.Client;
import com.example.demo.client.infraestructure.ClientRepository;
import com.example.demo.config.JwtService;
import com.example.demo.exceptions.EventAlreadyBookException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.reunion.dto.*;
import com.example.demo.reunion.infraestructure.ReunionRepository;
import com.google.api.services.calendar.model.Event;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReunionService {
    private final ReunionRepository reunionRepository;
    private final AdminRepository adminRepository;
    private final ClientRepository clientRepository;
    private final GoogleCalendarService googleCalendarService;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final ModelMapper modelMapper;

    public CalendarEventDto createAvailableEvent(CreateEventRequestDto request) {
        Long adminId = jwtService.getCurrentUserId();
        Reunion reunion = new Reunion();
        reunion.setUrl("");
        reunion.setDescription(request.getDescription());
        reunion.setTag(request.getTag());
        reunion.setSessionDate(request.getStartTime());
        reunion.setHoraInicio(request.getStartTime());
        reunion.setHoraFin(request.getEndTime());
        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Admin no encontrado"));
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
        Long clientId = jwtService.getCurrentUserId();
        Client client = clientRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        Reunion reunion = reunionRepository.findById(request.getEventId()).orElseThrow(() -> new ResourceNotFoundException("Reunion no encontrada"));

        if (reunion.getClient() != null) {
            throw new EventAlreadyBookException("Evento ya está reservado");
        }

        Event calendarEvent = null;
        LocalDateTime endTime = reunion.getHoraFin();
            
        String eventDescription = reunion.getDescription();
        try {
            calendarEvent = googleCalendarService.createEventWithoutAttendees(
                    "Sesión de " + reunion.getTag(),
                    eventDescription,
                    reunion.getSessionDate(),
                    endTime
            );
        } catch (IOException e) {

        }

        if (calendarEvent != null) {
            reunion.setGoogleEventId(calendarEvent.getId());
            reunion.setUrl(calendarEvent.getHangoutLink());
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
                googleCalendarService.deleteEvent(reunion.getGoogleEventId());
            } catch (IOException e) {
            }
        }

        reunion.setIsCancelled(true);
        reunionRepository.save(reunion);
    }

    //Si soy cliente obtengo mis reus pendientes
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
