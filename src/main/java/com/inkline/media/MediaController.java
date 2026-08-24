package com.inkline.media;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final Path avatarsRoot =
            Paths.get("uploads", "avatars").toAbsolutePath().normalize();

    private final Path articlesRoot =
            Paths.get("uploads", "articles").toAbsolutePath().normalize();

    @GetMapping("/avatars/{fileName:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable String fileName) {
        return serve(avatarsRoot, fileName);
    }

    @GetMapping("/articles/{fileName:.+}")
    public ResponseEntity<Resource> article(@PathVariable String fileName) {
        return serve(articlesRoot, fileName);
    }

    private ResponseEntity<Resource> serve(Path root, String fileName) {
        // Only a generated filename is accepted. Never allow path traversal.
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        try {
            String contentType = Files.probeContentType(file);
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
