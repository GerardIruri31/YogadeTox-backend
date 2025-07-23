package com.example.demo.tbIntermediateAdminQa.domain;

import com.example.demo.admin.domain.Admin;
import com.example.demo.qa.domain.QA;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "qa_admin_relation")
public class QAAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // o AUTO
    private Long id;

    @ManyToOne
    @JoinColumn(name = "qa_id", nullable = false)
    private QA qa;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

}
