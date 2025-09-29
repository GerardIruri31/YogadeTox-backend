package com.example.demo.admin.domain;


import com.example.demo.curso.domain.Curso;
import com.example.demo.reunion_temp.domain.Reunion;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import com.example.demo.user.domain.User;
import com.example.demo.content.domain.Content;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class Admin extends User {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    @OneToMany(mappedBy = "admin")
    private List<QAAdmin> qaAdmins = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "admin")
    private List<Reunion> reunion = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "admin")
    private List<Content> content = new ArrayList<>();

    @OneToMany(cascade = {PERSIST, MERGE}, mappedBy = "admin")
    private List<Curso> cursos = new ArrayList<>();

}
