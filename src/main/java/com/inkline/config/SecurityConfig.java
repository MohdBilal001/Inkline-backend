package com.inkline.config;

import com.inkline.auth.CustomOAuth2UserService;
import com.inkline.auth.JwtAuthenticationFilter;
import com.inkline.auth.OAuth2SuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            CustomOAuth2UserService oAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler) {

        this.jwtFilter = jwtFilter;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(cors -> {})

            // OAuth2 login needs a short-lived HTTP session while Google
            // redirects the browser to Google and back. JWT API calls
            // remain stateless.
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/contact"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/users/me"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/users/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/users/me"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users/me/avatar"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/users/me/avatar"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.GET,
                    "/uploads/avatars/**",
                    "/uploads/articles/**",
                    "/api/media/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/articles",
                    "/api/articles/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/articles"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/articles/*/clap",
                    "/api/articles/*/repost",
                    "/api/articles/*/comments"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/articles/**"
                ).authenticated()

                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
            )

            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(
            List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "https://inkline-frontend-production.up.railway.app"
            )
        );

        config.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
        );

        config.setAllowedHeaders(
            List.of("*")
        );

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            config
        );

        return source;
    }
}
