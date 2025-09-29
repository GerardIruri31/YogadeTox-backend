package com.example.demo.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.demo.user.domain.Role;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${my.jwt.code}")
    private String secret;
    
    private final UserService userService;

    public String extractUsername(String token){
        return JWT.decode(token).getSubject();
    }

    // Se puede usar para obtener userID
    public Long extractUserId(String token) {
        return JWT.decode(token).getClaim("userId").asLong();
    }

    public Role getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Role.FREE;

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getRole();
        }

        String roleStr = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_FREE");
        return Role.valueOf(roleStr.replace("ROLE_", ""));
    }


    // Se puede usar para obtener userID
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof User user) {
                return user.getId();
            } else if (auth.getPrincipal() instanceof String userIdStr) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    // Obtener email del usuario actual
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof User user) {
                return user.getEmail();
            } else if (auth.getPrincipal() instanceof String userIdStr) {
                // Ahora el principal es el userId como String
                // Necesitamos obtener el email desde el JWT o desde la base de datos
                try {
                    Long userId = Long.parseLong(userIdStr);
                    // Buscar el usuario en la base de datos para obtener el email
                    User user = userService.findById(userId);
                    return user != null ? user.getEmail() : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }


    public String generatedToken(UserDetails userDetails){
        User user = (User) userDetails;
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 * 60 * 60 * 24);
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withSubject(user.getEmail())
                .withClaim("userId", user.getId())
                .withClaim("email", user.getEmail())
                .withClaim("role", "ROLE_" + user.getRole().name())
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    public void validateToken(String token) {
        JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
    }
}
