package com.example.demo.content.infraestructure;

import com.example.demo.content.domain.Content;
import com.example.demo.content.domain.Idiom;
import com.example.demo.curso.domain.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content,Long> {

    @Query("""
           SELECT c
           FROM Content c
           WHERE c.idiom = :idiom
             AND (COALESCE(:isPremium, FALSE) = TRUE OR c.isPremium = FALSE)
           """)
    List<Content> findByIsPremiumAndIdiom(@Param("isPremium") Boolean isPremium,
                                          @Param("idiom") Idiom idiom);
    List<Content> findByCursoId(Long cursoId);
    boolean existsByTitle(String title);


    @Query("""
           SELECT c
           FROM Content c
           WHERE c.idiom = :idiom
             AND c.tag = :tag
             AND (COALESCE(:isPremium, FALSE) = TRUE OR c.isPremium = FALSE)
           """)
    List<Content> findByTagIsPremiumAndIdiom(@Param("tag") String tag,@Param("isPremium") Boolean isPremium,
                                          @Param("idiom") Idiom idiom);


}
