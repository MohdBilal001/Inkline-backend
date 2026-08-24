package com.inkline.article;

import com.inkline.auth.UserPrincipal;
import com.inkline.user.User;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleRepository articles;
    private final ArticleClapRepository claps;
    private final ArticleRepostRepository reposts;
    private final CommentRepository comments;
    private final ArticleCoverStorage coverStorage;

    public ArticleController(
            ArticleRepository articles,
            ArticleClapRepository claps,
            ArticleRepostRepository reposts,
            CommentRepository comments,
            ArticleCoverStorage coverStorage) {

        this.articles = articles;
        this.claps = claps;
        this.reposts = reposts;
        this.comments = comments;
        this.coverStorage = coverStorage;
    }

    // ---------------------------------------------------------------------
    // FEED
    // ---------------------------------------------------------------------

    @GetMapping
    public Page<ArticleResponse> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(Math.max(size, 1), 50);

        return articles
                .findByStatusOrderByPublishedAtDesc(
                        "PUBLISHED",
                        PageRequest.of(Math.max(page, 0), safeSize)
                )
                .map(ArticleResponse::from);
    }

    // ---------------------------------------------------------------------
    // GET ARTICLE
    // ---------------------------------------------------------------------

    @GetMapping("/{slug}")
    public ResponseEntity<ArticleResponse> get(
            @PathVariable String slug) {

        return articles.findBySlug(slug)
                .map(article ->
                        ResponseEntity.ok(
                                ArticleResponse.from(article)
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    // ---------------------------------------------------------------------
    // CREATE ARTICLE
    // ---------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<ArticleResponse> create(
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User author = principal.getUser();

        Article article = new Article();

        article.setTitle(request.title().trim());
        article.setSlug(uniqueSlug(request.title()));
        article.setExcerpt(request.excerpt());
        article.setContent(request.content());
        article.setCoverUrl(request.coverUrl());

        article.setReadingTime(
                request.readingTime() == null
                        ? 1
                        : Math.max(1, request.readingTime())
        );

        article.setAuthor(author);

        String status =
                "PUBLISHED".equalsIgnoreCase(request.status())
                        ? "PUBLISHED"
                        : "DRAFT";

        article.setStatus(status);

        if ("PUBLISHED".equals(status)) {
            article.setPublishedAt(Instant.now());
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ArticleResponse.from(
                                articles.save(article)
                        )
                );
    }

    // ---------------------------------------------------------------------
    // EDIT ARTICLE — AUTHOR ONLY, WITHIN 10 MINUTES
    // ---------------------------------------------------------------------

    @PutMapping("/{slug}")
    @Transactional
    public ResponseEntity<?> update(
            @PathVariable String slug,
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Article article = articles.findBySlug(slug).orElse(null);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        if (!article.getAuthor().getId().equals(principal.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only the author can edit this article.");
        }

        Instant deadline = article.getCreatedAt().plusSeconds(10 * 60);
        if (Instant.now().isAfter(deadline)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Articles can only be edited within 10 minutes of publishing.");
        }

        article.setTitle(request.title().trim());
        article.setExcerpt(request.excerpt());
        article.setContent(request.content());
        article.setCoverUrl(request.coverUrl());
        article.setReadingTime(
                request.readingTime() == null
                        ? 1
                        : Math.max(1, request.readingTime())
        );

        // Keep the existing slug so already-shared article URLs continue to work.
        if ("PUBLISHED".equalsIgnoreCase(request.status())) {
            article.setStatus("PUBLISHED");
            if (article.getPublishedAt() == null) {
                article.setPublishedAt(Instant.now());
            }
        }

        return ResponseEntity.ok(ArticleResponse.from(articles.save(article)));
    }

    // ---------------------------------------------------------------------
    // ARTICLE COVER IMAGE
    // ---------------------------------------------------------------------

    @PostMapping("/cover-image")
    public ResponseEntity<?> uploadCoverImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String url = coverStorage.store(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Could not save cover image.");
        }
    }

    // ---------------------------------------------------------------------
    // CLAP / UNCLAP
    // ---------------------------------------------------------------------

    @PostMapping("/{slug}/clap")
    @Transactional
    public ResponseEntity<ArticleResponse> toggleClap(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal) {

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = principal.getUser();

        boolean alreadyClapped =
                claps.existsByArticleIdAndUserId(
                        article.getId(),
                        user.getId()
                );

        if (alreadyClapped) {

            // UNCLAP
            claps.deleteByArticleIdAndUserId(
                    article.getId(),
                    user.getId()
            );

            // Keep the count from going negative.
            if (article.getLikes() > 0) {
                // Article currently has no decrementLikes() method,
                // so we update it through the helper below.
                article.decrementLikes();
            }

        } else {

            // CLAP
            claps.save(
                    new ArticleClap(article, user)
            );

            article.incrementLikes();
        }

        articles.save(article);

        return ResponseEntity.ok(
                ArticleResponse.from(article)
        );
    }

    // ---------------------------------------------------------------------
    // CLAP STATUS
    // ---------------------------------------------------------------------

    @GetMapping("/{slug}/clap-status")
    public ResponseEntity<Map<String, Boolean>> clapStatus(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal) {

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        if (principal == null) {
            return ResponseEntity.ok(
                    Map.of("clapped", false)
            );
        }

        boolean clapped =
                claps.existsByArticleIdAndUserId(
                        article.getId(),
                        principal.getUser().getId()
                );

        return ResponseEntity.ok(
                Map.of("clapped", clapped)
        );
    }


    // ---------------------------------------------------------------------
    // REPOST / UNREPOST
    // ---------------------------------------------------------------------

    @PostMapping("/{slug}/repost")
    @Transactional
    public ResponseEntity<ArticleResponse> toggleRepost(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal) {

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = principal.getUser();

        boolean alreadyReposted = reposts.existsByArticleIdAndUserId(
                article.getId(),
                user.getId()
        );

        if (alreadyReposted) {
            reposts.deleteByArticleIdAndUserId(article.getId(), user.getId());
            article.decrementReposts();
        } else {
            reposts.save(new ArticleRepost(article, user));
            article.incrementReposts();
        }

        articles.save(article);

        return ResponseEntity.ok(ArticleResponse.from(article));
    }

    // ---------------------------------------------------------------------
    // REPOST STATUS
    // ---------------------------------------------------------------------

    @GetMapping("/{slug}/repost-status")
    public ResponseEntity<Map<String, Boolean>> repostStatus(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal) {

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        if (principal == null) {
            return ResponseEntity.ok(Map.of("reposted", false));
        }

        boolean reposted = reposts.existsByArticleIdAndUserId(
                article.getId(),
                principal.getUser().getId()
        );

        return ResponseEntity.ok(Map.of("reposted", reposted));
    }

    // ---------------------------------------------------------------------
    // GET COMMENTS
    // ---------------------------------------------------------------------

    @GetMapping("/{slug}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable String slug) {

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        List<CommentResponse> result =
                comments
                        .findByArticleIdOrderByCreatedAtAsc(
                                article.getId(),
                                PageRequest.of(0, 100)
                        )
                        .map(CommentResponse::from)
                        .getContent();

        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------------
    // ADD COMMENT
    // ---------------------------------------------------------------------

    @PostMapping("/{slug}/comments")
    @Transactional
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String slug,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        Comment comment = comments.save(
                new Comment(
                        article,
                        principal.getUser(),
                        request.content().trim()
                )
        );

        article.incrementComments();
        articles.save(article);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommentResponse.from(comment));
    }

    // ---------------------------------------------------------------------
    // DELETE COMMENT
    // ---------------------------------------------------------------------

    @DeleteMapping("/{slug}/comments/{commentId}")
    @Transactional
    public ResponseEntity<Void> deleteComment(
            @PathVariable String slug,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        Comment comment =
                comments.findByIdAndArticleId(
                        commentId,
                        article.getId()
                ).orElse(null);

        if (comment == null) {
            return ResponseEntity.notFound().build();
        }

        // Only the person who created the comment can delete it.
        if (!comment.getAuthor().getId()
                .equals(principal.getUser().getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        comments.delete(comment);

        article.decrementComments();
        articles.save(article);

        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // DELETE ARTICLE
    // ---------------------------------------------------------------------

    @DeleteMapping("/{slug}")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Article article = articles.findBySlug(slug).orElse(null);

        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        // Only the article author can delete it.
        if (!article.getAuthor().getId()
                .equals(principal.getUser().getId())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }

        comments.deleteByArticleId(article.getId());
        claps.deleteByArticleId(article.getId());
        reposts.deleteByArticleId(article.getId());

        articles.delete(article);

        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // SLUG
    // ---------------------------------------------------------------------

    private String uniqueSlug(String title) {

        String base = Normalizer
                .normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (base.isBlank()) {
            base = "article";
        }

        String slug = base;
        int i = 2;

        while (articles.findBySlug(slug).isPresent()) {
            slug = base + "-" + i++;
        }

        return slug;
    }
}