package com.example.demo.auth.application;

import com.example.demo.auth.domain.AuthService;
import com.example.demo.auth.dto.*;
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

    @GetMapping("/grant-code")
    public ResponseEntity<AuthResponseDto> grantCode(
            @RequestParam("code") String code,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "authuser", required = false) String authUser,
            @RequestParam(value = "prompt", required = false) String prompt) {
        AuthResponseDto response = authService.googleLogin(code);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PostMapping("/forget-password")
    public ResponseEntity<Void> olvidoPassword(@RequestBody SolicitarResetPasswordDto dto) {
        authService.solicitarResetPassword(dto.getEmail());
        return ResponseEntity.ok().build(); // respuesta neutra
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordDto dto) {
        authService.resetPassword(dto.getToken(), dto.getNuevaPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponseDto> validateToken() {
        // El JWT ya fue validado por el filtro, solo devolvemos la info del usuario
        AuthResponseDto response = authService.getCurrentUserInfo();
        return ResponseEntity.ok(response);
    }

}
