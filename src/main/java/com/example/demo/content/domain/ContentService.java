package com.example.demo.content.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.content.dto.ContentRequestDto;
import com.example.demo.content.dto.ContentResponse;
import com.example.demo.curso.dto.CursoConContenidosDto;

import com.example.demo.content.infraestructure.ContentRepository;
import com.example.demo.curso.domain.Curso;
import com.example.demo.exceptions.ResourceAlreadyExists;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {
    private final ContentRepository contentRepository;
    private final ModelMapper modelMapper;
    private final AdminRepository adminRepository;

    // Obtener All Contenido (incluye agrupamiento por curso)
    public List<Object> getContent(Boolean isPremium, Idiom idiom) {
        List<Content> freeContent = contentRepository.findByIsPremiumAndIdiom(isPremium, idiom);
        List<Content> sinCurso = freeContent.stream()
                .filter(c -> c.getCurso() == null)
                .collect(Collectors.toList());

        Map<Curso, List<Content>> agrupados = freeContent.stream()
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

    // Crear Contenido
    public ContentResponse createContent(Long adminId, ContentRequestDto contentRequestDto) {
        if (contentRepository.existsByTitle(contentRequestDto.getTitle())) {
            throw new ResourceAlreadyExists("Ya existe un curso con el título: " + contentRequestDto.getTitle());
        }
        Content content = modelMapper.map(contentRequestDto, Content.class);
        Admin admin = adminRepository.findById(adminId).orElseThrow(()->new ResourceNotFoundException("Admin no encontrado"));
        content.setAdmin(admin);
        Content saved = contentRepository.save(content);
        return modelMapper.map(saved, ContentResponse.class);
    }

    // Actualizar Contenido
    public ContentResponse updateContent(Long contentId, ContentRequestDto dto){
        Content content = contentRepository.findById(contentId).orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));
        if(dto.getTitle() != null) {
            if (contentRepository.existsByTitle(dto.getTitle())) {
                throw new IllegalArgumentException("Ya existe un curso con el título: " + dto.getTitle());
            }
            content.setTitle(dto.getTitle());
        }
        if(dto.getIdiom() != null) content.setIdiom(dto.getIdiom());
        if (dto.getDuration() != null) content.setDuration(dto.getDuration());
        if(dto.getDescriptionKeywords() != null) content.setDescriptionKeywords(dto.getDescriptionKeywords());
        if(dto.getTag() != null) content.setTag(dto.getTag());
        if(dto.getIsPremium() != null) content.setIsPremium(dto.getIsPremium());
        Content saved = contentRepository.save(content);
        return modelMapper.map(saved,ContentResponse.class);
    }

    // Eliminar Contenido
    public void deleteContent(Long contentId){
        Content content = contentRepository.findById(contentId).orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));
        contentRepository.delete(content);
    }


}