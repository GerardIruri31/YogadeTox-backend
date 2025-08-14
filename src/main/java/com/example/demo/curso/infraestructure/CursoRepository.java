package com.example.demo.curso.infraestructure;

import com.example.demo.curso.domain.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso,Long> {
    Optional<Curso> findByTitleAndIsPremium(String title,boolean isPremium);
    List<Curso> findByTagAndIsPremium(String tag, boolean isPremium);
    boolean existsByTitle(String title);
}
