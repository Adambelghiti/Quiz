package com.march.backend.controller;

import com.march.backend.dto.AuthRequest;
import com.march.backend.dto.AuthResponse;
import com.march.backend.model.AuthUser;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Registration and login endpoints. On success both return an
 * {@link AuthResponse} containing a signed JWT that the client sends back as
 * {@code Authorization: Bearer <token>}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

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
        String username = normalizeUsername(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword();

        String usernameError = validateUsername(username);
        if (usernameError != null) {
            return badRequest(usernameError);
        }
        if (password.isEmpty()) {
            return badRequest("Password is required");
        }
        if (password.length() < 4) {
            return badRequest("Password must be at least 4 characters long");
        }
        // Case-insensitive: "Alice" and "alice" are the same account.
        if (authUserRepository.existsByUsernameIgnoreCase(username)) {
            return conflict("Username already exists");
        }

        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        try {
            AuthUser saved = authUserRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(saved));
        } catch (DataIntegrityViolationException ex) {
            // The check above cannot close the race between two concurrent
            // registrations, so the unique constraint is the real source of
            // truth. Report it as a conflict rather than a 500.
            return conflict("Username already exists");
        }
    }

    /** Authenticate an existing user and return a fresh token. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword();

        AuthUser user = username.isEmpty() ? null : authUserRepository.findByUsernameIgnoreCase(username);
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

    /** Trim a raw username; never returns {@code null}. */
    private String normalizeUsername(String raw) {
        return raw == null ? "" : raw.trim();
    }

    /** Return a human-readable problem with the username, or {@code null} when it is valid. */
    private String validateUsername(String username) {
        if (username.isEmpty()) {
            return "Username is required";
        }
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            return "Username must be between " + MIN_USERNAME_LENGTH + " and "
                    + MAX_USERNAME_LENGTH + " characters long";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username may only contain letters, digits, '.', '_' and '-'";
        }
        return null;
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, String>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
    }
}
