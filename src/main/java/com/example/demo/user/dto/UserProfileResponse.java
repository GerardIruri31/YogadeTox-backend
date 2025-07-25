package com.example.demo.user.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class UserProfileResponse{
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private ZonedDateTime createdAt;
}