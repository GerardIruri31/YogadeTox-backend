package com.example.demo.reunion.application;

import com.example.demo.reunion.domain.Reunion;
import com.example.demo.reunion.domain.ReunionService;
import com.example.demo.reunion.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final ReunionService reunionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/events")
    public ResponseEntity<CalendarEventDto> createAvailableEvent(@RequestBody CreateEventRequestDto request) {
        CalendarEventDto event = reunionService.createAvailableEvent(request);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events/available")
    public ResponseEntity<List<CalendarEventDto>> getAvailableEvents() {
        List<CalendarEventDto> events;
        events = reunionService.getAvailableEvents();
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events/book")
    public ResponseEntity<BookEventResponseDto> bookEvent(@RequestBody BookEventRequestDto request) {
        BookEventResponseDto response = reunionService.bookEvent(request);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/reunions/{reunionId}")
    public ResponseEntity<Void> cancelReunion(@PathVariable Long reunionId) {
        reunionService.cancelReunion(reunionId);
        return ResponseEntity.ok().build();
    }

    //Si soy cliente obtengo mis reus pendientes
    @GetMapping("/reunions/client/{clientId}")
    public ResponseEntity<List<MyReunionDto>> getClientReunions() {
        List<MyReunionDto> reunions = reunionService.getClientReunions();
        return ResponseEntity.ok(reunions);
    }

}
