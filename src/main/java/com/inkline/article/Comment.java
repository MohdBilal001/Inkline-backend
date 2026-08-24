package com.inkline.article;

import com.inkline.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "comments", indexes = {
        @Index(
                name = "idx_comments_article_created",
                columnList = "article_id,created_at"
        )
})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    protected Comment() {}

    public Comment(Article article, User author, String content) {
        this.article = article;
        this.author = author;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Article getArticle() {
        return article;
    }

    public User getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}