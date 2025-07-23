package com.example.demo.clientHistorial.domain;


import com.example.demo.client.domain.Client;
import com.example.demo.video.domain.Video;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Historial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private Client client;

    @ManyToMany
    @JoinTable(
            name = "historial_video",
            joinColumns = @JoinColumn(name = "historial_id"),
            inverseJoinColumns = @JoinColumn(name = "video_id")
    )
    private List<Video> video;


}
