package com.example.demo.tbIntermediateAdminQa.infraestructure;

import com.example.demo.admin.domain.Admin;
import com.example.demo.qa.domain.QA;
import com.example.demo.tbIntermediateAdminQa.domain.QAAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QAAdminRepository extends JpaRepository<QAAdmin, Long> {
    List<QAAdmin> findByQaIdOrderByRespondedAtAsc(Long qaId);

}
