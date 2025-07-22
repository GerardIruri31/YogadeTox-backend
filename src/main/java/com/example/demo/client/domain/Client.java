package com.example.demo.client.domain;


import com.example.demo.admin.domain.Admin;
import com.example.demo.adminAuditoria.domain.AdminAuditoria;
import com.example.demo.qa.domain.QA;
import com.example.demo.reunión.application.Reunion;
import com.example.demo.user.domain.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Client extends User {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client")
    private List<QA> qa;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client")
    private List<Reunion> reunion;





}
