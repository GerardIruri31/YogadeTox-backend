package com.example.demo.clientHistorial.infraestructure;

import com.example.demo.clientHistorial.domain.Historial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialRepository extends JpaRepository<Historial,Long> {
}
