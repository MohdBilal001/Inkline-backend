package com.inkline.article;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleClapRepository extends JpaRepository<ArticleClap, Long> {

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleId(Long articleId);
}