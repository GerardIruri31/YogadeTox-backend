package com.example.demo.tbIntermediateAdminQa.infraestructure;

import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface tbIntermediateAdminQaRepository extends JpaRepository<QAAdmin, Long> {
}
