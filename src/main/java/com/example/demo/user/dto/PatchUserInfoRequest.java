package com.example.demo.user.dto;

import lombok.Data;

@Data
public class PatchUserInfoRequest {
    private String firstName;
    private String lastName;
    private String username;
    private String phoneNumber;
}
