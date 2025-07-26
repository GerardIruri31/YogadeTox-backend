package com.example.demo.curso.domain;

import com.example.demo.content.domain.Content;
import com.example.demo.content.domain.Idiom;
import com.example.demo.content.dto.ContentResponse;
import com.example.demo.content.infraestructure.ContentRepository;
import com.example.demo.curso.dto.CourseRequestDto;
import com.example.demo.curso.dto.CourseResponseDto;
import com.example.demo.curso.dto.CursoConContenidosDto;
import com.example.demo.curso.infraestructure.CursoRepository;
import com.example.demo.exceptions.ResourceAlreadyExists;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CourseService {
    private final ContentRepository contentRepository;
    private final ModelMapper modelMapper;
    private final CursoRepository cursoRepository;

    public List<?> freeContent(Boolean isPremium, Idiom idiom) {
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

    //Funciones ADMIN
    //Crear curso
    public CourseRequestDto createCourse(CourseRequestDto curso){ //Falta verificar si es admin y para eso falta security
        Curso nuevoCurso = new Curso();
        nuevoCurso.setTitle(curso.getTitle());
        nuevoCurso.setIdiom(curso.getIdiom());
        nuevoCurso.setDuration(curso.getDescription());
        nuevoCurso.setTag(curso.getTag());
        nuevoCurso.setIsPremium(curso.getIsPremium());
        cursoRepository.save(nuevoCurso);
        CourseRequestDto courseRequestDto = modelMapper.map(nuevoCurso, CourseRequestDto.class);
        return courseRequestDto;
    }

    //Actualizar curso
    public void updateCourse(Long cursoId, CourseRequestDto courseRequestDto) {
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        if (courseRequestDto.getTitle() != null) curso.setTitle(courseRequestDto.getTitle());
        if (courseRequestDto.getIdiom() != null) curso.setIdiom(courseRequestDto.getIdiom());
        if (courseRequestDto.getDescription() != null) curso.setDescription(courseRequestDto.getDescription());
        if (courseRequestDto.getTag() != null) curso.setTag(courseRequestDto.getTag());
        if (courseRequestDto.getIsPremium() != null) curso.setIsPremium(courseRequestDto.getIsPremium());

        cursoRepository.save(curso);
    }

    //Agregar Contenido al curso
    public void assignContentToCourse(Long cursoId, Long contendId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        Content content = contentRepository.findById(contendId).orElseThrow(() -> new ResourceNotFoundException("Contenido no encontrado"));

        for(Content content1: curso.getContent()){
            if(content1.getTitle().equalsIgnoreCase(content.getTitle())){
                throw new ResourceAlreadyExists("Este contenido ya esta en el curso");
            }
        }

        content.setCurso(curso);
        contentRepository.save(content);
    }


    //Borrar Curso
    public void deleteCourse(Long cursoId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        cursoRepository.delete(curso);
    }

    //Funciones tipo search:
    public CourseResponseDto getByTittle(String title){
        Curso curso = cursoRepository.findByTitle(title).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        return modelMapper.map(curso, CourseResponseDto.class);
    }

    public CourseResponseDto getByTag(String tag){
        Curso curso = cursoRepository.findByTag(tag).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        return modelMapper.map(curso, CourseResponseDto.class);
    }

    public void calculateDuration(Long cursoId){
        Curso curso = cursoRepository.findById(cursoId).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
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
        }catch (NumberFormatException e){
            throw new IllegalArgumentException("Duracion invalida: " + duration);
        }
        return 0;
    }

    /*
    Formato ingresado	Interpretación actual
    "80"	            80 minutos (1h 20m)
    "12:30"	            12 minutos, 30 segundos
    "01:45:10"	        1 hora, 45 minutos, 10 segundos
    */

    //Calcular duration y asignar duration
    /*
    Consultar con el compa y el jefe
    como calcularemos la duration del curso? o sera definida al momento de crear??
     */
}
