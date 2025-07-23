package com.example.demo.reunión.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.client.domain.Client;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "reunion")
public class Reunion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "url", nullable = false)
    private String url;
    @Column(name = "description", nullable = false)
    private String description;
    @Column(name = "tag")
    private String tag;
    @Column(name = "sesionDate", nullable = false)
    private LocalDateTime sesionDate;
    @Column(name = "hora_inicio")
    private LocalDateTime horaInicio;
    @Column(name = "cost", nullable = false)
    private Double cost;
    @Column(name = "is_cancelled",nullable = false)
    private Boolean isCancelled;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false) // columna FK en la tabla video
    private Client client;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false) // columna FK en la tabla video
    private Admin admin;



}