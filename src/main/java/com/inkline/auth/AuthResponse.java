package com.inkline.auth;

import com.inkline.user.User;

public record AuthResponse(String token, UserResponse user) {
    public record UserResponse(
            Long id,
            String name,
            String username,
            String email,
            String bio,
            String avatarUrl
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getBio(),
                    user.getAvatarUrl()
            );
        }
    }
}
