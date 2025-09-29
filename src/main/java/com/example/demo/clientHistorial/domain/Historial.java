package com.example.demo.clientHistorial.domain;


import com.example.demo.client.domain.Client;
import com.example.demo.content.domain.Content;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Historial {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private Client client;

    @ManyToMany
    @JoinTable(
            name = "historial_content",
            joinColumns = @JoinColumn(name = "historial_id"),
            inverseJoinColumns = @JoinColumn(name = "content_id")
    )
    private List<Content> content;


}
