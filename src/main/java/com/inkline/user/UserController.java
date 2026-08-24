package com.inkline.user;

import com.inkline.auth.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final UserAvatarStorage avatarStorage;

    public UserController(UserRepository users, UserAvatarStorage avatarStorage) {
        this.users = users;
        this.avatarStorage = avatarStorage;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserResponse> getMe(
            Authentication authentication
    ) {
        if (authentication == null ||
                authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        return users.findByEmailIgnoreCase(authentication.getName())
                .map(user -> ResponseEntity.ok(
                        AuthResponse.UserResponse.from(user)
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthResponse.UserResponse> getUser(
            @PathVariable Long id
    ) {
        return users.findById(id)
                .map(user -> ResponseEntity.ok(
                        AuthResponse.UserResponse.from(user)
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<AuthResponse.UserResponse> getByUsername(
            @PathVariable String username
    ) {
        return users.findByUsernameIgnoreCase(username)
                .map(user -> ResponseEntity.ok(
                        AuthResponse.UserResponse.from(user)
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody UserUpdateRequest request
    ) {
        if (authentication == null ||
                authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmailIgnoreCase(authentication.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        String name = request.name() == null
                ? ""
                : request.name().trim();

        String username = request.username() == null
                ? ""
                : request.username().trim();

        String bio = request.bio() == null
                ? ""
                : request.bio().trim();

        String avatarUrl = request.avatarUrl() == null
                ? ""
                : request.avatarUrl().trim();

        if (name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Name is required");
        }

        if (username.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Username is required");
        }

        if (username.length() > 20) {
            return ResponseEntity.badRequest()
                    .body("Username must be 20 characters or less");
        }

        if (!username.matches("^[a-zA-Z0-9._]+$")) {
            return ResponseEntity.badRequest()
                    .body("Special characters in username are not allowed. Only letters, numbers, dot (.) and underscore (_) are allowed.");
        }

        if (name.length() > 80) {
            return ResponseEntity.badRequest()
                    .body("Name must be 80 characters or less");
        }

        if (bio.length() > 500) {
            return ResponseEntity.badRequest()
                    .body("Bio must be 500 characters or less");
        }

        if (avatarUrl.length() > 500) {
            return ResponseEntity.badRequest()
                    .body("Avatar URL must be 500 characters or less");
        }

        var existingUsername =
                users.findByUsernameIgnoreCase(username);

        if (existingUsername.isPresent()
                && !existingUsername.get().getId().equals(user.getId())) {
            return ResponseEntity.badRequest()
                    .body("Username is already taken");
        }

        user.setName(name);
        user.setUsername(username.toLowerCase());
        user.setBio(bio);
        user.setAvatarUrl(
                avatarUrl.isBlank() ? null : avatarUrl
        );

        User saved = users.save(user);

        return ResponseEntity.ok(
                AuthResponse.UserResponse.from(saved)
        );
    }
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String oldAvatar = user.getAvatarUrl();
            String newAvatar = avatarStorage.store(file);
            user.setAvatarUrl(newAvatar);
            User saved = users.save(user);
            avatarStorage.delete(oldAvatar);
            return ResponseEntity.ok(AuthResponse.UserResponse.from(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Could not save profile photo.");
        }
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<?> deleteAvatar(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmailIgnoreCase(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String oldAvatar = user.getAvatarUrl();
        user.setAvatarUrl(null);
        User saved = users.save(user);
        avatarStorage.delete(oldAvatar);

        return ResponseEntity.ok(AuthResponse.UserResponse.from(saved));
    }

}