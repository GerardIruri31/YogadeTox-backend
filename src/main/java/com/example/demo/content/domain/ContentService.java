package com.example.demo.content.domain;

import com.example.demo.AWS_S3.domain.S3MultipartService;
import com.example.demo.AWS_S3.dto.MediaUploadResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
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
    private final S3MultipartService s3MultipartService;
    private static final Logger logger = LoggerFactory.getLogger(ContentService.class);

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


    // Get Content By Tag
    public List<Object> getContentByTag(String tag,Boolean isPremium, Idiom idiom) {
        List<Content> freeContent = contentRepository.findByTagIsPremiumAndIdiom(tag,isPremium, idiom);
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
    public ContentResponse createContent(Long adminId, ContentRequestDto contentRequestDto, MultipartFile file) {
        if (contentRepository.existsByTitle(contentRequestDto.getTitle())) throw new ResourceAlreadyExists("Ya existe un curso con el título: " + contentRequestDto.getTitle());
        Admin admin = adminRepository.findById(adminId).orElseThrow(()->new ResourceNotFoundException("Admin no encontrado"));
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Debe proporcionar un archivo de video o audio");
        try {
            // Determinar la carpeta según el tipo de contenido
            String folder = s3MultipartService.determineFolder(file.getContentType());
            // Subir el archivo a S3
            MediaUploadResponse uploadResponse = s3MultipartService.uploadMediaFile(file, folder);
            if (!uploadResponse.isSuccess()) {
                throw new RuntimeException("Error subiendo archivo a S3: " + uploadResponse.getErrorMessage());
            }
            Content content = modelMapper.map(contentRequestDto, Content.class);
            content.setKeyS3Bucket(uploadResponse.getFileName()); // Este es el key único en S3
            content.setAdmin(admin);
            Content saved = contentRepository.save(content);
            logger.info("Contenido creado exitosamente. ID: {}, S3 Key: {}, Método de subida: {}", saved.getId(), uploadResponse.getFileName(), uploadResponse.getUploadMethod());
            return modelMapper.map(saved, ContentResponse.class);
        } catch (Exception e) {
            logger.error("Error creando contenido para admin {}: {}", adminId, e.getMessage(), e);
            throw new RuntimeException("Error interno al crear contenido: " + e.getMessage(), e);
        }
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
        String key = content.getKeyS3Bucket();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("No se pudo determinar la clave S3 del contenido");
        }
        s3MultipartService.deleteObject(key);
        contentRepository.delete(content);
    }


}