package com.example.demo.clientHistorial.domain;

import com.example.demo.clientHistorial.infraestructure.HistorialRepository;
import com.example.demo.content.domain.Content;
import com.example.demo.content.dto.ContentResponse;
import com.example.demo.content.infraestructure.ContentRepository;
import com.example.demo.curso.domain.Curso;
import com.example.demo.curso.dto.CursoConContenidosDto;
import com.example.demo.curso.infraestructure.CursoRepository;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.qa.domain.QAService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistorialService {
    @Autowired
    private HistorialRepository historialRepository;
    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private CursoRepository cursoRepository;

    // FALTA MEJORAR
    public void addContentToHistorial(Long historial_id, Long recursoId, NombreRecurso nombreRecurso) {
        Historial historial = historialRepository.findById(historial_id).orElseThrow(() -> new ResourceNotFoundException("Historial no encontrado"));
        List<Content> contenidoHistorial = historial.getContent();
        // Creado para comparar: O(n)
        Set<Content> existentes = new HashSet<>(contenidoHistorial);
        switch (nombreRecurso) {
            case CONTENT -> {
                Content content = contentRepository.findById(recursoId).orElseThrow(() -> new ResourceNotFoundException("Content no encontrado"));
                if (existentes.add(content)) {
                    contenidoHistorial.add(content);
                }
            }
            case CURSO -> {
                Curso curso = cursoRepository.findById(recursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
                curso.getContent().stream().filter(existentes::add).forEach(contenidoHistorial::add);
            }
            default -> throw new IllegalArgumentException("Tipo de recurso no soportado: " + nombreRecurso);
        }
        historialRepository.save(historial);
    }

    public List<Object> getClientHistorial(Long h_id) {
        Historial historial = historialRepository.findById(h_id)
                .orElseThrow(() -> new ResourceNotFoundException("Historial no encontrado"));
        List<Content> contentList = historial.getContent();
        List<Content> sinCurso = contentList.stream()
                .filter(c -> c.getCurso() == null)
                .collect(Collectors.toList());

        Map<Curso, List<Content>> agrupados = contentList.stream()
                .filter(c -> c.getCurso() != null)
                .collect(Collectors.groupingBy(Content::getCurso));

        List<Object> response = new ArrayList<>();
        for (Content content : sinCurso) {
            ContentResponse dto = modelMapper.map(content, ContentResponse.class);
            response.add(dto);
        }

        for (Map.Entry<Curso, List<Content>> entry : agrupados.entrySet()) {
            Map<String, Object> identifyCurso = new HashMap<>();
            Curso curso = entry.getKey();
            CursoConContenidosDto cursoDto = modelMapper.map(curso, CursoConContenidosDto.class);
            List<ContentResponse> contenidosDto = entry.getValue().stream()
                    .map(content -> modelMapper.map(content, ContentResponse.class))
                    .collect(Collectors.toList());
            cursoDto.setContenidos(contenidosDto);
            identifyCurso.put("curso", cursoDto);
            response.add(identifyCurso);
        }
        return response;
    }

}