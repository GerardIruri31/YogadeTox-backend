package com.example.demo.reunion.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateEventRequestDto {
    private String summary;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String attendeeEmail;
    private Long adminId;
    private Double cost;
    private String tag;
}
