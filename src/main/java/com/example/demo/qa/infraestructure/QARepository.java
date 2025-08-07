package com.example.demo.qa.infraestructure;

import com.example.demo.qa.domain.QA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QARepository extends JpaRepository<QA, Long> {
    List<QA> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<QA> findByIsRespondedOrderByCreatedAtDesc(boolean isResponded);
}
