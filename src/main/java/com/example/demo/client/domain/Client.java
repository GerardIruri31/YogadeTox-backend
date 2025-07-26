package com.example.demo.client.domain;


import com.example.demo.qa.domain.QA;
import com.example.demo.reunión.domain.Reunion;
import com.example.demo.user.domain.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Client extends User {
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client")
    private List<QA> qa = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client")
    private List<Reunion> reunion = new ArrayList<>();





}
