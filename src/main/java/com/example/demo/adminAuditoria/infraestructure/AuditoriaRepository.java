package com.example.demo.adminAuditoria.infraestructure;

import com.example.demo.adminAuditoria.domain.AdminAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<AdminAuditoria,Long> {
}
