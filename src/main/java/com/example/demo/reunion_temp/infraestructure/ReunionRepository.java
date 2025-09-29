package com.example.demo.reunion_temp.infraestructure;

import com.example.demo.reunion_temp.domain.Reunion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReunionRepository extends JpaRepository<Reunion,Long> {
    List<Reunion> findByClientId(Long clientId);

    List<Reunion> findByAdminId(Long adminId);

    Optional<Reunion> findByGoogleEventId(String googleEventId);

    List<Reunion> findByIsCancelledFalse();

    List<Reunion> findByClientIdIsNullAndSessionDateBetweenAndIsCancelledFalse(LocalDateTime startDate, LocalDateTime endDate);

    // Métodos faltantes que usa ReunionService
    List<Reunion> findByClientIdIsNullAndSessionDateAfterAndIsCancelledFalse(LocalDateTime date);

    List<Reunion> findBySessionDateAfterAndIsCancelledFalse(LocalDateTime date);

    List<Reunion> findByClientIdIsNullAndSessionDateBeforeAndIsCancelledFalse(LocalDateTime date);
}
