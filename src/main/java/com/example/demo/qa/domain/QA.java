package com.example.demo.qa.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.client.domain.Client;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Entity
public class QA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @Column(name = "isResponded", nullable = false)
    private boolean isResponded;
    @Column(name = "message", nullable = false)
    private String message;


    @ManyToOne
    private Client client;

    @OneToMany(mappedBy = "qa")
    private List<QAAdmin> qaAdmins;
}
