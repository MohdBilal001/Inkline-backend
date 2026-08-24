package com.inkline.user;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class UserAvatarStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final Cloudinary cloudinary;

    public UserAvatarStorage(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Profile photo must be 5 MB or smaller.");
        }

        String contentType = file.getContentType();
        if (!EXTENSIONS.containsKey(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, GIF, and WebP images are allowed.");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "inkline/avatars",
                            "resource_type", "image",
                            "use_filename", false,
                            "unique_filename", true,
                            "overwrite", false
                    )
            );

            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new IOException("Cloudinary did not return an image URL.");
            }

            return secureUrl.toString();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Could not upload profile photo to Cloudinary.", e);
        }
    }

    public void delete(String avatarUrl) {
        // Keep existing assets intact during the migration to Cloudinary.
        // Old local /api/media URLs remain served by MediaController.
    }
}
