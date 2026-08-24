package com.inkline.article;

import com.inkline.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "article_reposts", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_article_repost_article_user",
                columnNames = {"article_id", "user_id"}
        )
})
public class ArticleRepost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected ArticleRepost() {}

    public ArticleRepost(Article article, User user) {
        this.article = article;
        this.user = user;
    }
}
