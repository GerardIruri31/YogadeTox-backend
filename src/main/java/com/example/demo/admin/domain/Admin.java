package com.example.demo.admin.domain;


import com.example.demo.adminAuditoria.domain.AdminAuditoria;
import com.example.demo.qa.domain.QA;
import com.example.demo.reunión.application.Reunion;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import com.example.demo.user.domain.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Admin extends User {
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @OneToMany(mappedBy = "admin")
    private List<AdminAuditoria> auditorias;

    @ManyToMany(mappedBy = "admin")
    private List<QA> qa;

    @OneToMany(mappedBy = "admin")
    private List<QAAdmin> qaAdmins;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "admin")
    private List<Reunion> reunion;

}
