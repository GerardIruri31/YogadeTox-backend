package com.example.demo.curso.infraestructure;

import com.example.demo.content.domain.Content;
import com.example.demo.content.domain.Idiom;
import com.example.demo.curso.domain.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso,Long> {


    Optional<Curso> findByTitleAndIsPremium(String title,boolean isPremium);

    @Query("""
           SELECT c
           FROM Curso c
           WHERE c.idiom = :idiom
             AND c.tag = :tag
             AND (COALESCE(:isPremium, FALSE) = TRUE OR c.isPremium = FALSE)
           """)
    List<Curso> findByTagAndIsPremiumAndIdiom(@Param("tag") String tag,@Param("isPremium") Boolean isPremium,
                                              @Param("idiom") Idiom idiom);
    boolean existsByTitle(String title);
}
