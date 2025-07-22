package com.example.demo.qa.infraestructure;

import com.example.demo.qa.domain.QA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QARepository extends JpaRepository<QA, Long> {
}
