package com.inkline.auth;

import com.inkline.user.User;
import com.inkline.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${inkline.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/$", "");
    }

    public void requestReset(String email) {
        users.findByEmailIgnoreCase(email.trim())
                .ifPresent(this::createAndSend);
    }

    private void createAndSend(User user) {
        if (mailUsername == null || mailUsername.isBlank()) {
            throw new IllegalStateException("Mail is not configured.");
        }

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setPasswordResetTokenHash(hash(token));
        user.setPasswordResetExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        users.save(user);

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailUsername);
        mail.setTo(user.getEmail());
        mail.setSubject("Reset your Inkline password");
        mail.setText(
                "Hi " + user.getName() + ",\n\n"
                + "We received a request to reset your Inkline password.\n\n"
                + "Reset your password here:\n"
                + resetUrl + "\n\n"
                + "This link expires in 15 minutes and can only be used once.\n\n"
                + "If you did not request this, you can safely ignore this email.\n\n"
                + "— Inkline"
        );
        mailSender.send(mail);
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }

        User user = users.findByPasswordResetTokenHash(hash(token))
                .orElseThrow(this::invalidToken);

        if (user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            user.setPasswordResetTokenHash(null);
            user.setPasswordResetExpiresAt(null);
            users.save(user);
            throw invalidToken();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        users.save(user);
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired.");
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
