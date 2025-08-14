package com.example.demo.auth.application;

import com.example.demo.auth.domain.AuthService;
import com.example.demo.auth.dto.AuthResponseDto;
import com.example.demo.auth.dto.LoginRequestDto;
import com.example.demo.auth.dto.RegisterRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        AuthResponseDto response = authService.login(loginRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody RegisterRequestDto registerRequestDto) {
        AuthResponseDto response = authService.register(registerRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/admin/register")
    public ResponseEntity<AuthResponseDto> createAdmin(@RequestBody RegisterRequestDto registerRequestDto) {
        AuthResponseDto response = authService.createAdmin(registerRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
    Endpoints futuros para implementar:
    POST /auth/social/google
    POST /auth/social/facebook
    POST /auth/social/apple
    POST /auth/refresh-token
    */
}
