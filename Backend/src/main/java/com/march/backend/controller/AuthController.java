package com.march.backend.controller;

import com.march.backend.dto.AuthRequest;
import com.march.backend.dto.AuthResponse;
import com.march.backend.model.AuthUser;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Registration and login endpoints. On success both return an
 * {@link AuthResponse} containing a signed JWT that the client sends back as
 * {@code Authorization: Bearer <token>}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AuthUserRepository authUserRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Register a new user and immediately return a token (auto-login). */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            return badRequest("Username and password are required");
        }
        if (password.length() < 4) {
            return badRequest("Password must be at least 4 characters long");
        }
        if (authUserRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }

        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        AuthUser saved = authUserRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(saved));
    }

    /** Authenticate an existing user and return a fresh token. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        AuthUser user = username.isEmpty() ? null : authUserRepository.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
        return ResponseEntity.ok(buildResponse(user));
    }

    /** Return the currently authenticated user. Requires a valid token. */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthUser user = authUserRepository.findByUsername(authentication.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("username", user.getUsername(), "role", user.getRole()));
    }

    private AuthResponse buildResponse(AuthUser user) {
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
