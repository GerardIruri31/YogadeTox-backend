package com.example.demo.admin.domain;


import com.example.demo.adminAuditoria.domain.AdminAuditoria;
import com.example.demo.reunión.domain.Reunion;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import com.example.demo.user.domain.User;
import com.example.demo.content.domain.Content;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Admin extends User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToMany(mappedBy = "admin")
    private List<AdminAuditoria> auditorias = new ArrayList<>();


    @OneToMany(mappedBy = "admin")
    private List<QAAdmin> qaAdmins = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "admin")
    private List<Reunion> reunion = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "admin")
    private List<Content> content = new ArrayList<>();

}
