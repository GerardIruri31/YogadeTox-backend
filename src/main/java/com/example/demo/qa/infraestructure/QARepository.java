package com.example.demo.qa.infraestructure;

import com.example.demo.qa.domain.QA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QARepository extends JpaRepository<QA, Long> {
    List<QA> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<QA> findByIsRespondedOrderByCreatedAtDesc(boolean isResponded);
    
    // Nuevas consultas para conversaciones
    List<QA> findByClientIdAndIsActiveTrueOrderByLastMessageTimeDesc(Long clientId);
    List<QA> findByIsActiveTrueOrderByLastMessageTimeDesc();
    List<QA> findByIsActiveTrueAndIsRespondedFalseOrderByCreatedAtDesc();
    List<QA> findByIsActiveTrueAndClientIdOrderByLastMessageTimeDesc(Long clientId);
    
    // Consultas paginadas
    Page<QA> findByIsActiveTrue(Pageable pageable);
    Page<QA> findByClientIdAndIsActiveTrue(Long clientId, Pageable pageable);
    
    // Consultas de conteo
    Long countByIsActiveTrue();
    Long countByIsActiveTrueAndIsRespondedFalse();
    Long countDistinctClientIdByIsActiveTrue();
    
    // Búsqueda
    List<QA> findByClientFirstNameContainingIgnoreCaseOrClientLastNameContainingIgnoreCaseOrTitleContainingIgnoreCase(
            String firstName, String lastName, String title, Pageable pageable);
}
