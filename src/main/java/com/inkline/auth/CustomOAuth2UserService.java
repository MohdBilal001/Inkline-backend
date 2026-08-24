package com.inkline.auth;

import com.inkline.user.User;
import com.inkline.user.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(
            UserRepository users,
            PasswordEncoder passwordEncoder) {

        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request)
            throws OAuth2AuthenticationException {

        DefaultOAuth2UserService delegate =
                new DefaultOAuth2UserService();

        OAuth2User oauthUser =
                delegate.loadUser(request);

        String email =
                oauthUser.getAttribute("email");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"),
                    "OAuth provider did not return an email address"
            );
        }

        String providerName =
                oauthUser.getAttribute("name");

        final String name =
                providerName == null || providerName.isBlank()
                        ? email.substring(0, email.indexOf("@"))
                        : providerName;

        users.findByEmailIgnoreCase(email)
                .orElseGet(() -> createUser(email, name));

        return oauthUser;
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