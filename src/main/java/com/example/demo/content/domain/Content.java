package com.example.demo.content.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.clientHistorial.domain.Historial;
import com.example.demo.curso.domain.Curso;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "idiom", nullable = false)
    @Enumerated(EnumType.STRING)
    private Idiom idiom;
    @Column(name = "keyS3Bucket", nullable = false)
    private String keyS3Bucket;
    @Column(name = "duration", nullable = false)
    private String duration;
    // NO es obligatorio
    @Column(name = "descriptionKeywords")
    private String descriptionKeywords;
    @Column(name = "tag", nullable = false)
    private String tag;
    private Boolean isPremium;
    @ManyToMany(mappedBy = "content")
    private List<Historial> historial;
    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;
    @ManyToOne
    @JoinColumn(name = "curso_id")
    private Curso curso;
}