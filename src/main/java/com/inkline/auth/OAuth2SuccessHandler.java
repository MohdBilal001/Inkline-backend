
package com.inkline.auth;

import com.inkline.user.User;
import com.inkline.user.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public OAuth2SuccessHandler(
            UserRepository users,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {

        this.users = users;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        String email =
                oauthUser.getAttribute("email");

        if (email == null || email.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Google did not return an email address"
            );
            return;
        }

        final String normalizedEmail =
                email.trim().toLowerCase();

        String name =
                oauthUser.getAttribute("name");

        if (name == null || name.isBlank()) {
            name = normalizedEmail.substring(0, normalizedEmail.indexOf("@"));
        }

        final String finalName = name;

        User user = users.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createUser(normalizedEmail, finalName));

        String token =
                jwtService.generateToken(
                        new UserPrincipal(user)
                );

        // The HTTP session was only needed to survive the redirect dance
        // with Google. From here on, every API call must be authenticated
        // by the JWT alone. If we leave the session (and its
        // OAuth2AuthenticationToken, whose getName() returns the
        // provider's "sub" rather than the email) sitting behind the
        // browser's JSESSIONID cookie, Spring Security's session filter
        // will restore that OAuth2AuthenticationToken on every future
        // request BEFORE JwtAuthenticationFilter runs, so
        // JwtAuthenticationFilter's "authentication == null" guard sees a
        // non-null Authentication and never applies the JWT identity.
        // Invalidating the session here removes that stale authentication
        // so JwtAuthenticationFilter is free to authenticate by JWT.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        String redirect =
                "http://localhost:5173/oauth-success?token="
                        + URLEncoder.encode(
                                token,
                                StandardCharsets.UTF_8
                        );

        response.sendRedirect(redirect);
    }

    private User createUser(
            String email,
            String name) {

        User user = new User();

        user.setName(name);
        user.setEmail(email);

        user.setUsername(
                createUniqueUsername(name)
        );

        user.setPasswordHash(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
        );

        return users.save(user);
    }

    private String createUniqueUsername(String name) {

        String base = name
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");

        if (base.isBlank()) {
            base = "writer";
        }

        String username = base;
        int counter = 1;

        while (users.existsByUsernameIgnoreCase(username)) {
            username = base + counter;
            counter++;
        }

        return username;
    }
}

