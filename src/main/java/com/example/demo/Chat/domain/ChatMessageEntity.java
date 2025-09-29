package com.example.demo.Chat.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.client.domain.Client;
import com.example.demo.qa.domain.QA;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@Data
public class ChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Contenido del msj
    @Column
    private String content;
    // Hora del mensaje
    @Column
    private ZonedDateTime timestamp;
    // Enum del usuario
    @Column
    @Enumerated(EnumType.STRING)
    private SenderType senderType; // CLIENT o ADMIN
    // Si es el primer mensaje del chat
    @Column(name = "is_first_message")
    private Boolean isFirstMessage = false;
    // Tipo de mensaje
    @Column(name = "message_type")
    private String messageType = "TEXT";
    // ID de la pregunta inicial que pertenece msj en el chat
    @ManyToOne
    @JoinColumn(name = "qa_id", nullable = false)
    private QA qa;
    // ID del client o ID del admin
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;
    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = true)
    private Admin admin;
}

