package com.example.demo.curso.domain;

import com.example.demo.config.JwtService;
import com.example.demo.content.domain.Content;
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
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CourseService {
    private final ContentRepository contentRepository;
    private final ModelMapper modelMapper;
    private final CursoRepository cursoRepository;
    private final JwtService jwtService;

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
    public List<CourseResponseDto> getCourseByTag(String tag) {
        Role role = jwtService.getCurrentUserRole();
        boolean isPremium = !role.FREE.equals(role);
        List<Curso> cursos = cursoRepository.findByTagAndIsPremium(tag,isPremium);
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

    //Crear curso
    public CourseResponseDto createCourse(CourseRequestDto curso) {
        if (cursoRepository.existsByTitle(curso.getTitle())) {
            throw new IllegalArgumentException("Ya existe un curso con el título: " + curso.getTitle());
        }
        Curso nuevoCurso = modelMapper.map(curso, Curso.class);
        nuevoCurso.setDuration("0");
        cursoRepository.save(nuevoCurso);
        return modelMapper.map(nuevoCurso, CourseResponseDto.class);
    }

    //Actualizar curso
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

    //Borrar Curso
    public void deleteCourse(Long cursoId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        cursoRepository.delete(curso);
    }

    @Transactional
    // Agregar Contenido al Curso
    public void assignContentToCourse(Long cursoId, Long contendId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        Content content = contentRepository.findById(contendId).orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));
        if (content.getTitle() == null || curso.getContent() == null) {
            throw new IllegalArgumentException("Datos inválidos");
        }
        if (curso.getContent().stream().anyMatch(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(content.getTitle()))) {
            throw new ResourceAlreadyExists("Este contenido ya esta en el curso");
        }
        content.setCurso(curso);
        curso.getContent().add(content); // Sincroniza memoria
        contentRepository.save(content);
        calculateDuration(curso);
        cursoRepository.save(curso);
    }

    // Eliminar Contenido del Curso
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



    private void calculateDuration(Curso curso){
        int totalSeconds = 0;
        for(Content content: curso.getContent()){
            String duration = content.getDuration();
            totalSeconds += parseDurationToSeconds(duration);
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        String formattedDuration = String.format("%dh %dm", hours, minutes);
        curso.setDuration(formattedDuration);
        cursoRepository.save(curso);
    }

    private int parseDurationToSeconds(String duration){
        String[] parts = duration.split(":");
        try{
            if(parts.length == 3){
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            } else if(parts.length == 1){
                return Integer.parseInt(parts[0]) * 60;
            }
        } catch (NumberFormatException e){
            throw new IllegalArgumentException("Duracion invalida: " + duration);
        }
        return 0;
    }

    /*
    //Forma de calcular duration:
    Formato ingresado	Interpretación actual
    "80"	            80 minutos (1h 20m)
    "12:30"	            12 minutos, 30 segundos
    "01:45:10"	        1 hora, 45 minutos, 10 segundos
    */

}
