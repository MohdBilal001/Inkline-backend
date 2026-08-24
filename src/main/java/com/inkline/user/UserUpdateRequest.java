package com.inkline.user;

public record UserUpdateRequest(
        String name,
        String username,
        String bio,
        String avatarUrl
) {
}