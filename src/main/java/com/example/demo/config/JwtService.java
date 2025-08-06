package com.example.demo.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public String generatedToken(UserDetails userDetails){
        User user = (User) userDetails;
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 * 60 * 60 + 24);
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withSubject(user.getEmail())
                .withClaim("email", user.getEmail())
                .withClaim("role", "ROLE_" + user.getRole().name())
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    public void validateToken(String token, String userEmail){
        JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        UserDetails userDetails = userService.userDetailsService().loadUserByUsername(userEmail);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
    }
}
