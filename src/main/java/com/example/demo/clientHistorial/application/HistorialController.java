package com.example.demo.clientHistorial.application;

import com.example.demo.clientHistorial.domain.Historial;
import com.example.demo.clientHistorial.domain.HistorialService;
import com.example.demo.clientHistorial.domain.NombreRecurso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/historial")
public class HistorialController {
    @Autowired
    private HistorialService historialService;

    // CUANDO CLIENT VISUALIZE CONTENT -> AGREGARLO A SU HISTORIAL
    @PreAuthorize("hasRole('FREE')")
    @PostMapping("/addContent/{historial_id}/{recursoId}")
    public ResponseEntity<Void> addContent(@PathVariable Long historial_id, @PathVariable Long recursoId, @RequestParam NombreRecurso nombreRecurso) {
        historialService.addContentToHistorial(historial_id, recursoId, nombreRecurso);
        return ResponseEntity.noContent().build();
    }

    // CLIENT VISUALIZA SU HISTORIAL DE CONTENIDO
    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/getContent")
    public ResponseEntity<List<Object>> getHistorial(@RequestParam Long h_id) {
        return ResponseEntity.ok(historialService.getClientHistorial(h_id));
    }

}
