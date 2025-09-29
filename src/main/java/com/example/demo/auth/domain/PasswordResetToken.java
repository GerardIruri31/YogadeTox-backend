package com.example.demo.auth.domain;

import com.example.demo.user.domain.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User usuario;

  @Column(nullable = false, unique = true, length = 64) // sha-256 hex
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiraEn;

  @Column(nullable = false)
  private boolean usado = false;

  private Instant usadoEn;
  private Instant creadoEn = Instant.now();
}
