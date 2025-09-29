package com.example.demo.reunion_temp.application;


import com.example.demo.reunion_temp.domain.ReunionService;
import com.example.demo.reunion_temp.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
public class CalendarController {
    @Autowired
    private ReunionService reunionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/events")
    public ResponseEntity<?> createAvailableEvent(@RequestBody CreateEventRequestDto request) {
        try {
            System.out.println("=== CREANDO EVENTO DISPONIBLE ===");
            System.out.println("Request recibido: " + request);
            
            CalendarEventDto event = reunionService.createAvailableEvent(request);
            
            System.out.println("✅ Evento creado exitosamente: " + event);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            System.err.println("=== ERROR EN CREATE AVAILABLE EVENT ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            
            // Devolver el error real en lugar de un 500 genérico
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error creando evento",
                "message", e.getMessage(),
                "details", e.getClass().getSimpleName()
            ));
        }
    }

    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/events/available")
    public ResponseEntity<List<CalendarEventDto>> getAvailableEvents() {
        List<CalendarEventDto> events;
        events = reunionService.getAvailableEvents();
        return ResponseEntity.ok(events);
    }

    @PreAuthorize("hasRole('FREE')")
    @PostMapping("/events/book")
    public ResponseEntity<BookEventResponseDto> bookEvent(@RequestBody BookEventRequestDto request) {
        BookEventResponseDto response = reunionService.bookEvent(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('FREE')")
    @DeleteMapping("/reunions/{reunionId}")
    public ResponseEntity<Void> cancelReunion(@PathVariable Long reunionId) {
        reunionService.cancelReunion(reunionId);
        return ResponseEntity.ok().build();
    }

    // ENDPOINT DE PRUEBA - SIN AUTENTICACIÓN
    @PostMapping("/events/test")
    public ResponseEntity<CalendarEventDto> createAvailableEventTest(@RequestBody CreateEventRequestDto request) {
        try {
            System.out.println("=== PRUEBA CREATE EVENT ===");
            System.out.println("Request recibido: " + request);
            
            // Crear evento usando email del JWT
            CalendarEventDto event = reunionService.createAvailableEventTest(request, null);
            
            System.out.println("Evento creado exitosamente: " + event);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            System.err.println("=== ERROR EN CREATE EVENT ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PreAuthorize("hasRole('FREE')")
    //Si soy cliente obtengo mis reus pendientes
    @GetMapping("/reunions/client/{clientId}")
    public ResponseEntity<List<MyReunionDto>> getClientReunions() {
        List<MyReunionDto> reunions = reunionService.getClientReunions();
        return ResponseEntity.ok(reunions);
    }

}