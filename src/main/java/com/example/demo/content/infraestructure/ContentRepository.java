package com.example.demo.content.infraestructure;

import com.example.demo.content.domain.Content;
import com.example.demo.content.domain.Idiom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content,Long> {
    List<Content> findByIsPremiumAndIdiom(Boolean isPremium, Idiom idiom);
    List<Content> findByCursoId(Long cursoId);
    boolean existsByTitle(String title);

}
