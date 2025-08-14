package com.example.demo.content.application;

import com.example.demo.content.domain.ContentService;
import com.example.demo.content.dto.ContentRequestDto;
import com.example.demo.content.dto.ContentResponse;
import com.example.demo.curso.domain.CourseService;
import com.example.demo.content.domain.Idiom;
import com.example.demo.curso.dto.CourseRequestDto;
import com.example.demo.curso.dto.CourseResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/content")
public class ContentController {
    @Autowired
    private ContentService contentService;

    @PreAuthorize("hasRole('FREE')")
    @GetMapping("/all")
    public ResponseEntity<List<Object>> getContent(@RequestParam boolean isPremium, @RequestParam Idiom idiom) {
        return ResponseEntity.ok(contentService.getContent(isPremium,idiom));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{adminId}")
    public ResponseEntity<ContentResponse> createContent(@PathVariable Long adminId, @RequestBody @Valid ContentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.createContent(adminId,dto));
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