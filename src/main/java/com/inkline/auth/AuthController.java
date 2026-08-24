package com.inkline.auth;

import com.inkline.user.User;
import com.inkline.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordResetService passwordResetService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email is already registered. Use another email.");
        }
        if (users.existsByUsernameIgnoreCase(request.username().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username is already taken. Use another username.");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setUsername(request.username().trim().toLowerCase());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User saved = users.save(user);
        UserPrincipal principal = new UserPrincipal(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(jwtService.generateToken(principal), AuthResponse.UserResponse.from(saved)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        // Always return 202 so callers cannot discover which emails are registered.
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        UserDetails details = users.findByEmailIgnoreCase(request.email())
                .map(UserPrincipal::new)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        User user = ((UserPrincipal) details).getUser();
        return new AuthResponse(
                jwtService.generateToken((UserPrincipal) details),
                AuthResponse.UserResponse.from(user)
        );
    }
}
