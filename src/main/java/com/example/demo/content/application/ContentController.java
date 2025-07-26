package com.example.demo.content.application;

import com.example.demo.content.domain.CourseService;
import com.example.demo.content.domain.Idiom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class ContentController {
    @Autowired
    private CourseService courseService;

    // Se necesita ID del user en token
    @GetMapping("/free")
    public ResponseEntity<List<?>> getFreeContent(@RequestParam Idiom idiom) {
        return ResponseEntity.ok(courseService.freeContent(false,idiom));
    }

    // Se necesita ID del user en token
    /*@GetMapping("/premium")
    public ResponseEntity<List<?>> getPremiumContent(@RequestParam Idiom idiom) {
        return ResponseEntity.ok(courseService.premiumContent(true,idiom));
    }*/
}