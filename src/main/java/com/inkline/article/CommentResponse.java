package com.inkline.article;

import com.inkline.user.User;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String content,
        Instant createdAt,
        Author author
) {

    public record Author(
            Long id,
            String username,
            String name,
            String bio,
            String avatarUrl
    ) {}

    public static CommentResponse from(Comment comment) {

        User u = comment.getAuthor();

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                new Author(
                        u.getId(),
                        u.getUsername(),
                        u.getName(),
                        u.getBio(),
                        u.getAvatarUrl()
                )
        );
    }
}