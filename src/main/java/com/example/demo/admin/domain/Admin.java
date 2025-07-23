package com.example.demo.admin.domain;


import com.example.demo.adminAuditoria.domain.AdminAuditoria;
import com.example.demo.reunión.domain.Reunion;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import com.example.demo.user.domain.User;
import com.example.demo.video.domain.Video;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Admin extends User {
    private Long id;
    @OneToMany(mappedBy = "admin")
    private List<AdminAuditoria> auditorias;


    @OneToMany(mappedBy = "admin")
    private List<QAAdmin> qaAdmins;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "admin")
    private List<Reunion> reunion;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "admin")
    private List<Video> video;



}
