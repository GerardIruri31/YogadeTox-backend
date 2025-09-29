package com.example.demo.config;

import com.example.demo.user.domain.User;
import com.example.demo.user.infraestructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {
    
    private final UserRepository userRepository;
    
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @GetMapping("/users/count")
    public long getUserCount() {
        return userRepository.count();
    }
    
    @GetMapping("/auth-info")
    public Map<String, Object> getAuthInfo(Authentication auth) {
        Map<String, Object> info = new HashMap<>();
        if (auth != null) {
            info.put("name", auth.getName());
            info.put("authorities", auth.getAuthorities());
            info.put("authenticated", auth.isAuthenticated());
            info.put("principal", auth.getPrincipal().getClass().getSimpleName());
            
            // Verificar si el name es un número (userId)
            try {
                Long userId = Long.valueOf(auth.getName());
                info.put("userId", userId);
                info.put("isUserId", true);
            } catch (NumberFormatException e) {
                info.put("isUserId", false);
                info.put("error", "El principal no es un userId válido: " + auth.getName());
            }
        } else {
            info.put("message", "No hay autenticación activa");
        }
        return info;
    }
}
