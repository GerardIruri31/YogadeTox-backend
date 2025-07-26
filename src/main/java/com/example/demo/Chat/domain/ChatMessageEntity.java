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

    private String content;

    private ZonedDateTime timestamp;

    private SenderType senderType; // CLIENT o ADMIN

    @ManyToOne
    @JoinColumn(name = "qa_id", nullable = false)
    private QA qa;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = true)
    private Admin admin;
}


/*
hmm xd
*/