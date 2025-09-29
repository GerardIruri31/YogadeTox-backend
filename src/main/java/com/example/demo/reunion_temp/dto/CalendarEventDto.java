package com.example.demo.reunion_temp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CalendarEventDto {
    private Long eventId;
    private String summary;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String meetingUrl;
    private boolean isAvailable;
}