package com.inkline.article;

import com.inkline.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "articles", indexes = {
        @Index(name = "idx_articles_published_at", columnList = "published_at"),
        @Index(name = "idx_articles_author_id", columnList = "author_id")
})
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "reading_time")
    private Integer readingTime = 1;

    @Column(nullable = false)
    private long likes = 0;

    @Column(nullable = false)
    private long comments = 0;

    @Column(nullable = false)
    private long reposts = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getExcerpt() { return excerpt; }
    public String getContent() { return content; }
    public String getCoverUrl() { return coverUrl; }
    public String getStatus() { return status; }
    public Integer getReadingTime() { return readingTime; }
    public long getLikes() { return likes; }
    public long getComments() { return comments; }
    public long getReposts() { return reposts; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public User getAuthor() { return author; }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReadingTime(Integer readingTime) {
        this.readingTime = readingTime;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public void incrementLikes() {
        this.likes++;
    }
    public void decrementLikes() {
        if (this.likes > 0) {
            this.likes--;
        }
    }

    public void incrementComments() {
        this.comments++;
    }

    public void incrementReposts() {
        this.reposts++;
    }

    public void decrementReposts() {
        if (this.reposts > 0) {
            this.reposts--;
        }
    }

    public void decrementComments() {
        if (this.comments > 0) {
            this.comments--;
        }
    }
}