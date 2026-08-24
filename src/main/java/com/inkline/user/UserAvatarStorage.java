package com.inkline.user;

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
public class UserAvatarStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final Path root = Paths.get("uploads", "avatars").toAbsolutePath().normalize();

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Profile photo must be 5 MB or smaller.");
        }

        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
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

        return "/api/media/avatars/" + fileName;
    }

    public void delete(String avatarUrl) {
        if (avatarUrl == null) {
            return;
        }

        final String prefix1 = "/uploads/avatars/";
        final String prefix2 = "/api/media/avatars/";

        String fileName;
        if (avatarUrl.startsWith(prefix1)) {
            fileName = avatarUrl.substring(prefix1.length());
        } else if (avatarUrl.startsWith(prefix2)) {
            fileName = avatarUrl.substring(prefix2.length());
        } else {
            return;
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            return;
        }

        try {
            Files.deleteIfExists(root.resolve(fileName).normalize());
        } catch (IOException ignored) {
            // A missing/unreadable old avatar should not prevent profile updates.
        }
    }
}
