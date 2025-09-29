package com.example.demo.content.application;

import com.example.demo.content.domain.ContentService;
import com.example.demo.content.dto.ContentRequestDto;
import com.example.demo.content.dto.ContentResponse;
import com.example.demo.curso.domain.CourseService;
import com.example.demo.content.domain.Idiom;
import com.example.demo.curso.dto.CourseRequestDto;
import com.example.demo.curso.dto.CourseResponseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/content")
public class ContentController {
    @Autowired
    private ContentService contentService;
    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);


    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/all")
    public ResponseEntity<List<Object>> getContent(@RequestParam boolean isPremium, @RequestParam Idiom idiom) {
        return ResponseEntity.ok(contentService.getContent(isPremium,idiom));
    }


    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/tag")
    public ResponseEntity<List<Object>> getContentByTag(@RequestParam String tag, @RequestParam boolean isPremium, @RequestParam Idiom idiom) {
        return ResponseEntity.ok(contentService.getContentByTag(tag,isPremium,idiom));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload/{adminId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentResponse> createContent(@PathVariable Long adminId, @RequestPart("contentData") @Valid ContentRequestDto dto, @RequestPart("file") MultipartFile file ) {
        try {
            ContentResponse response = contentService.createContent(adminId, dto, file);
            logger.info("Contenido creado exitosamente con ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creando contenido: {}", e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentResponse> updateCourse(@PathVariable Long contentId, @RequestBody @Valid ContentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(contentService.updateContent(contentId, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long contentId) {
        contentService.deleteContent(contentId);
        return ResponseEntity.noContent().build();
    }
}