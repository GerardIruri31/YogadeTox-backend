package com.example.demo.content.dto;

import com.example.demo.content.domain.Idiom;
import lombok.Data;

@Data
public class ContentResponse {
    private Long Id;
    private String title;
    private Idiom idiom;
    private String keyS3Bucket;
    private String duration;
    private String descriptionKeywords;
    private String tag;
    private Boolean isPremium;
}


