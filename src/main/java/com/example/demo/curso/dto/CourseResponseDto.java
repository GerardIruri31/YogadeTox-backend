package com.example.demo.curso.dto;

import com.example.demo.content.domain.Idiom;
import com.example.demo.content.dto.ContentResponse;
import lombok.Data;

import java.util.List;

@Data
public class CourseResponseDto {
    private Long id;
    private String title;
    private Idiom idiom;
    private String duration;
    private String description;
    private String tag;
    private Boolean isPremium;
    private String keyS3Bucket;
    private List<ContentResponse> content;
}
