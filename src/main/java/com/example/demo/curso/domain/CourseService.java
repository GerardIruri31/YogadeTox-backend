package com.example.demo.curso.domain;

import com.example.demo.AWS_S3.domain.S3MultipartService;
import com.example.demo.AWS_S3.dto.MediaUploadResponse;
import com.example.demo.admin.domain.Admin;
import com.example.demo.admin.infraestructure.AdminRepository;
import com.example.demo.config.JwtService;
import com.example.demo.content.domain.Content;
import com.example.demo.content.domain.ContentService;
import com.example.demo.content.domain.Idiom;
import com.example.demo.content.dto.ContentResponse;
import com.example.demo.content.infraestructure.ContentRepository;
import com.example.demo.curso.dto.CourseRequestDto;
import com.example.demo.curso.dto.CourseResponseDto;
import com.example.demo.curso.infraestructure.CursoRepository;
import com.example.demo.exceptions.ResourceAlreadyExists;
import com.example.demo.exceptions.ResourceNotFoundException;

import com.example.demo.user.domain.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CourseService {
    private static final Logger logger = LoggerFactory.getLogger(ContentService.class);
    private final ContentRepository contentRepository;
    private final ModelMapper modelMapper;
    private final CursoRepository cursoRepository;
    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final S3MultipartService s3MultipartService;


    // Funciones tipo search: (Optimización: Role diferencia busqueda de contenido free/premium)
    // Busqueda de Curso por título
    public CourseResponseDto getByTittle(String title) {
        Role role = jwtService.getCurrentUserRole();
        boolean isPremium = !role.FREE.equals(role);
        Curso curso = cursoRepository.findByTitleAndIsPremium(title,isPremium).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        CourseResponseDto cursoDto = modelMapper.map(curso, CourseResponseDto.class);

        List<Content> contenido = contentRepository.findByCursoId(curso.getId());
        List<ContentResponse> contentList = contenido.stream()
                .map(content -> modelMapper.map(content, ContentResponse.class))
                .collect(Collectors.toList());
        cursoDto.setContent(contentList);
        return cursoDto;
    }

    // Busqueda de Curso por tag
    public List<CourseResponseDto> getCourseByTag(String tag, Idiom idiom) {
        Role role = jwtService.getCurrentUserRole();
        boolean isPremium = !role.FREE.equals(role);
        List<Curso> cursos = cursoRepository.findByTagAndIsPremiumAndIdiom(tag,isPremium,idiom);
        if (cursos.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron cursos con el tag: " + tag);
        }
        return cursos.stream().map(curso -> {
            CourseResponseDto cursoDto = modelMapper.map(curso, CourseResponseDto.class);
            List<Content> contenido = contentRepository.findByCursoId(curso.getId());
            List<ContentResponse> contentList = contenido.stream()
                    .map(content -> modelMapper.map(content, ContentResponse.class))
                    .collect(Collectors.toList());
            cursoDto.setContent(contentList);
            return cursoDto;
        }).collect(Collectors.toList());

    }

    // Crear curso
    public CourseResponseDto createCourse(CourseRequestDto curso, Long adminId, MultipartFile file) {
        if (cursoRepository.existsByTitle(curso.getTitle())) throw new IllegalArgumentException("Ya existe un curso con el título: " + curso.getTitle());
        Admin admin = adminRepository.findById(adminId).orElseThrow(()->new ResourceNotFoundException("No se encontró Admin con ID " + adminId));
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Debe proporcionar un archivo de video o audio");
        try{
            // Determinar la carpeta según el tipo de contenido
            String folder = s3MultipartService.determineFolder(file.getContentType());
            // Subir el archivo a S3
            MediaUploadResponse uploadResponse = s3MultipartService.uploadMediaFile(file, folder);
            if (!uploadResponse.isSuccess()) {
                throw new RuntimeException("Error subiendo archivo a S3: " + uploadResponse.getErrorMessage());
            }
            Curso nuevoCurso = modelMapper.map(curso, Curso.class);
            nuevoCurso.setDuration("0");
            nuevoCurso.setAdmin(admin);
            nuevoCurso.setKeyS3Bucket(uploadResponse.getFileName());
            cursoRepository.save(nuevoCurso);
            CourseResponseDto dto = modelMapper.map(nuevoCurso, CourseResponseDto.class);
            logger.info("Curso creado exitosamente. ID: {}, S3 Key: {}, Método de subida: {}", dto.getId(), uploadResponse.getFileName(), uploadResponse.getUploadMethod());
            dto.setAdminId(adminId);
            return dto;
        } catch (Exception e) {
            logger.error("Error creando contenido para admin {}: {}", adminId, e.getMessage(), e);
            throw new RuntimeException("Error interno al crear contenido: " + e.getMessage(), e);
        }
    }

    // Actualizar curso
    public CourseResponseDto updateCourse(Long cursoId, CourseRequestDto courseRequestDto) {
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        if (courseRequestDto.getTitle() != null) {
            if (cursoRepository.existsByTitle(curso.getTitle())) {
                throw new IllegalArgumentException("Ya existe un curso con el título: " + curso.getTitle());
            }
            curso.setTitle(courseRequestDto.getTitle());
        }
        if (courseRequestDto.getIdiom() != null) curso.setIdiom(courseRequestDto.getIdiom());
        if (courseRequestDto.getDescription() != null) curso.setDescription(courseRequestDto.getDescription());
        if (courseRequestDto.getTag() != null) curso.setTag(courseRequestDto.getTag());
        if (courseRequestDto.getIsPremium() != null) curso.setIsPremium(courseRequestDto.getIsPremium());
        Curso saved = cursoRepository.save(curso);
        return modelMapper.map(saved,CourseResponseDto.class);
    }

    // Borrar Curso
    public void deleteCourse(Long cursoId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        List<Content> contenido = contentRepository.findByCursoId(cursoId);
        for (Content content : contenido) {
            content.setCurso(null);
        }
        String key = curso.getKeyS3Bucket();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("No se pudo determinar la clave S3 del curso");
        }
        s3MultipartService.deleteObject(key);
        cursoRepository.delete(curso);
    }

    @Transactional
    // Agregar Contenido al Curso
    public void assignContentToCourse(Long cursoId, Long contendId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        Content content = contentRepository.findById(contendId).orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));
        if (content.getTitle() == null ) throw new IllegalArgumentException("Datos inválidos");
        if (curso.getContent() == null) curso.setContent(new ArrayList<>());
        boolean alreadyIn = curso.getContent().stream().anyMatch(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(content.getTitle()));
        if (alreadyIn) throw new ResourceAlreadyExists("Este contenido ya está en el curso");
        content.setCurso(curso);
        curso.getContent().add(content); // Sincroniza memoria
        calculateDuration(curso);
        contentRepository.save(content);
        cursoRepository.save(curso);
    }

    // Desvincular Contenido del Curso
    @Transactional
    public void UnlinkCoursefromContent(Long cursoId, Long contendId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        Content content = contentRepository.findById(contendId).orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));
        if (curso.getContent() == null) {
            throw new IllegalArgumentException("Datos inválidos");
        }
        content.setCurso(null);
        curso.getContent().remove(content); // Sincroniza memoria
        contentRepository.save(content);
        calculateDuration(curso);
        cursoRepository.save(curso);
    }


    private void calculateDuration(Curso curso) {
        int totalSeconds = 0;
        for (Content c : curso.getContent()) {
            totalSeconds += parseDurationToSeconds(c.getDuration());
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        String formatted = String.format("%dh %dm", hours, minutes);
        curso.setDuration(formatted);
    }

    private int parseDurationToSeconds(String duration) {
        if (duration == null || duration.isBlank()) return 0;
        duration = duration.trim();
        // Acepta formatos: "HH:mm:ss", "mm:ss", "HH:mm", "90", "45m", "1h 30m"
        // Normaliza "1h 30m" y "45m" a "HH:mm:ss"/"mm:ss"
        if (duration.matches("\\d+\\s*h\\s*\\d+\\s*m")) { // "1h 30m"
            String[] hm = duration.toLowerCase().replaceAll("\\s+", "")
                    .replace("h", ":").replace("m", ":00").split(":");
            // queda "1:30:00"
            return Integer.parseInt(hm[0]) * 3600 + Integer.parseInt(hm[1]) * 60;
        }
        if (duration.matches("\\d+\\s*m")) { // "45m"
            int m = Integer.parseInt(duration.toLowerCase().replaceAll("\\D", ""));
            return m * 60;
        }
        if (duration.matches("\\d+")) { // "90" (minutos)
            return Integer.parseInt(duration) * 60;
        }

        String[] parts = duration.split(":");
        int[] x = new int[parts.length];
        for (int i = 0; i < parts.length; i++) x[i] = Integer.parseInt(parts[i].trim());

        switch (parts.length) {
            case 3: // HH:mm:ss
                return x[0] * 3600 + x[1] * 60 + x[2];
            case 2:
                return x[0] * 60 + x[1]; // mm:ss
            case 1: // "90" -> 90 minutos
                return x[0] * 60;
            default:
                throw new IllegalArgumentException("Duración inválida: " + duration);
        }
    }


    /*
    //Forma de calcular duration:
    Formato ingresado	Interpretación actual
    "80"	            80 minutos (1h 20m)
    "12:30"	            12 minutos, 30 segundos
    "01:45:10"	        1 hora, 45 minutos, 10 segundos
    */

}
