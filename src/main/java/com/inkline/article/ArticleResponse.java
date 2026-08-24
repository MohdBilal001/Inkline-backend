package com.inkline.article;

import com.inkline.user.User;

import java.time.Instant;

public record ArticleResponse(
        Long id,
        String slug,
        String title,
        String excerpt,
        String content,
        String coverUrl,
        String status,
        Integer readingTime,
        long likes,
        long comments,
        long reposts,
        Instant publishedAt,
        Instant createdAt,
        Author author
) {
    public record Author(Long id, String username, String name, String bio, String avatarUrl) {}

    public static ArticleResponse from(Article article) {
        User u = article.getAuthor();
        return new ArticleResponse(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getExcerpt(),
                article.getContent(),
                article.getCoverUrl(),
                article.getStatus(),
                article.getReadingTime(),
                article.getLikes(),
                article.getComments(),
                article.getReposts(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                new Author(u.getId(), u.getUsername(), u.getName(), u.getBio(), u.getAvatarUrl())
        );
    }
}
