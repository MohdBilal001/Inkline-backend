package com.inkline.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Comment> findByArticleIdOrderByCreatedAtAsc(
            Long articleId,
            PageRequest pageable
    );

    @EntityGraph(attributePaths = "author")
    Optional<Comment> findByIdAndArticleId(
            Long id,
            Long articleId
    );

    void deleteByArticleId(Long articleId);
}