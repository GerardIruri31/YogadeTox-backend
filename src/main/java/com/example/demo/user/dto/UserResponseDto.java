package com.example.demo.user.dto;

import com.example.demo.user.domain.Role;
import lombok.Data;

@Data
public class UserResponseDto {
    private Long id;
    private Role role;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
}