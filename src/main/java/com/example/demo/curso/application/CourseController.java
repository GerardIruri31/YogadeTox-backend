package com.example.demo.curso.application;

import com.example.demo.content.application.ContentController;
import com.example.demo.content.domain.Idiom;
import com.example.demo.curso.domain.CourseService;
import com.example.demo.curso.dto.CourseRequestDto;
import com.example.demo.curso.dto.CourseResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);

    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/byTitle")
    public ResponseEntity<CourseResponseDto> getByTitle(@RequestParam String title) {
        return ResponseEntity.ok(courseService.getByTittle(title));
    }

    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/byTag")
    public ResponseEntity<List<CourseResponseDto>> getByTag(@RequestParam Idiom idiom, @RequestParam String tag) {
        return ResponseEntity.ok(courseService.getCourseByTag(tag,idiom));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{adminId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponseDto> createCourse(@PathVariable Long adminId, @RequestPart("contentData") @Valid CourseRequestDto courseRequest, @RequestPart("file") MultipartFile file) {
        try {
            CourseResponseDto response = courseService.createCourse(courseRequest, adminId, file);
            logger.info("Curso creado exitosamente con ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creando contenido: {}", e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{cursoId}")
    public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable Long cursoId, @RequestBody @Valid CourseRequestDto courseRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(courseService.updateCourse(cursoId, courseRequest));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{cursoId}/assignContent/{contentId}")
    public ResponseEntity<Void> assignContentToCourse(@PathVariable Long cursoId, @PathVariable Long contentId) {
        courseService.assignContentToCourse(cursoId, contentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{cursoId}/unlinkContent/{contentId}")
    public ResponseEntity<Void> unlinkContentToCourse(@PathVariable Long cursoId, @PathVariable Long contentId) {
        courseService.UnlinkCoursefromContent(cursoId, contentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{cursoId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long cursoId) {
        courseService.deleteCourse(cursoId);
        return ResponseEntity.noContent().build();
    }


}