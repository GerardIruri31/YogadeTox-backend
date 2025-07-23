package com.example.demo.adminAuditoria.domain;

import com.example.demo.admin.domain.Admin;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
public class AdminAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @Column(name = "description", nullable = false)
    private String description;
    @Column(name = "isCommited", nullable = false)
    private IsCommited isCommited;
    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false) // columna FK en la tabla video
    private Admin admin;
}
