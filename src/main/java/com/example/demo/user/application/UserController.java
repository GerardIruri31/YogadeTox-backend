package com.example.demo.user.application;


import com.example.demo.auth.domain.AuthService;
import com.example.demo.auth.dto.AuthResponseDto;
import com.example.demo.auth.dto.LoginRequestDto;
import com.example.demo.auth.dto.RegisterRequestDto;
import com.example.demo.user.domain.UserService;
import com.example.demo.user.dto.PatchUserInfoRequest;
import com.example.demo.user.dto.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    // Se necesita ID del user en token
    @GetMapping("/profile/{id}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.userInfo(id));
    }

    // Se necesita ID del user en token
    @PatchMapping("/profile/{id}")
    public ResponseEntity<UserProfileResponse> PatchProfile(@PathVariable Long id, @RequestBody PatchUserInfoRequest dto) {
        return ResponseEntity.ok(userService.actualizeUserInfo(id, dto));
    }
}