package com.example.demo.reunión.infraestructure;

import com.example.demo.reunión.application.Reunion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReunionRepository extends JpaRepository<Reunion,Long> {
}
