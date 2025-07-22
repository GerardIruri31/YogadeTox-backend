package com.example.demo.curso.domain;

import com.example.demo.clientHistorial.domain.Historial;
import com.example.demo.reunión.application.Reunion;
import com.example.demo.video.domain.Idiom;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "idiom", nullable = false)
    private Idiom idiom;

    @Column(name = "keyS3Bucket", nullable = false)
    private String keyS3Bucket;

    @Column(name = "duration", nullable = false)
    private String duration;

    // NO es obligatorio
    @Column(name = "description")
    private String description;
    @Column(name = "tag", nullable = false)
    private String tag;
    private Boolean isPremium;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "video")
    private List<Curso> curso;



}