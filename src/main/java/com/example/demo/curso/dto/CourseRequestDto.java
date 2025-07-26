package com.example.demo.curso.dto;

import com.example.demo.content.domain.Idiom;
import lombok.Data;

@Data
public class CourseRequestDto {
    private String title;
    private Idiom idiom;
    //private String duration;
    private String description;
    private String tag;
    private Boolean isPremium;
}
