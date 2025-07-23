package com.example.demo.auth.dto;

import com.example.demo.user.domain.Role;
import lombok.Data;


@Data
public class RegisterRequestDto {
    private Role role = Role.FREE;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
}
