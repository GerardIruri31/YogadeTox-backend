package com.example.demo.auth.dto;

import lombok.Data;

@Data
public class ResetPasswordDto {
  private String token;
  private String nuevaPassword;
}