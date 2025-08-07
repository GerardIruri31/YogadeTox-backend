package com.example.demo.clientHistorial.application;

import com.example.demo.clientHistorial.domain.Historial;
import com.example.demo.clientHistorial.domain.HistorialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/historial")
public class HistorialController {
    @Autowired
    private HistorialService historialService;

    // CUANDO CLIENT VISUALIZE CONTENT -> AGREGARLO A SU HISTORIAL
    @PostMapping("/addContent")
    public ResponseEntity<String> addContent(@RequestParam Long h_id, @RequestParam Long contId) {
        return ResponseEntity.ok(historialService.addContentToHistorial(h_id, contId));
    }

    // CLIENT VISUALIZA SU HISTORIAL DE CONTENIDO
    @GetMapping("/getContent")
    public ResponseEntity<List<Object>> getHistorial(@RequestParam Long h_id) {
        return ResponseEntity.ok(historialService.getClientHistorial(h_id));
    }


}
