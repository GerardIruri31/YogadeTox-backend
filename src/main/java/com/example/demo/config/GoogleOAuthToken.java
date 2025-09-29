package com.example.demo.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "google_oauth_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleOAuthToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String organizerEmail; // Email del organizador (tu cuenta)
    
    @Column(columnDefinition = "TEXT")
    private String accessToken;
    
    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column
    private String scope;
    
    @Column
    private String tokenType;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
