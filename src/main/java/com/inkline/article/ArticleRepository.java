package com.inkline.article;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Article> findByStatusOrderByPublishedAtDesc(
            String status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "author")
    Optional<Article> findBySlug(String slug);
}