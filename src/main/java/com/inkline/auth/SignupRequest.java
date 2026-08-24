package com.inkline.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(min = 3, max = 20)
        @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Special characters in username are not allowed. Only letters, numbers, dot (.) and underscore (_) are allowed.")
        String username,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {}
