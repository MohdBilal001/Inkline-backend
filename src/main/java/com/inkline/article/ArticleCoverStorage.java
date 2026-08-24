package com.inkline.article;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class ArticleCoverStorage {

    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final Path root = Paths.get("uploads", "articles").toAbsolutePath().normalize();

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Cover image must be 10 MB or smaller.");
        }

        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Only JPG, PNG, GIF, and WebP images are allowed.");
        }

        Files.createDirectories(root);
        String fileName = UUID.randomUUID() + extension;
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Invalid upload path.");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target);
        }

        return "/api/media/articles/" + fileName;
    }
}
