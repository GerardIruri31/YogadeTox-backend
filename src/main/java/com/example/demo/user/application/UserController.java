package com.example.demo.user.application;



import com.example.demo.user.domain.UserService;
import com.example.demo.user.dto.PatchUserInfoRequest;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

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

    @GetMapping("/getMe")
    public ResponseEntity<UserResponseDto> getMyProfile(){
        return ResponseEntity.ok(userService.getMe());
    }
}