package com.inkline.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticleRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 500) String excerpt,
        @NotBlank String content,
        @Size(max = 500) String coverUrl,
        String status,
        Integer readingTime
) {}
