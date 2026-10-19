package com.march.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

/**
 * Creates and validates the JSON Web Tokens used for stateless authentication.
 * <p>
 * Tokens are signed with an HMAC-SHA256 key derived from {@code app.jwt.secret}.
 */
@Service
public class JwtService {

    /**
     * Placeholder shipped in {@code application.properties}. It is only meant
     * for local development; anywhere else {@link #APP_JWT_SECRET_ENV} must be
     * set or the application refuses to start.
     */
    public static final String DEFAULT_SECRET = "change-me-quizgame-development-secret-key-please-override";

    /** Environment variable that overrides {@code app.jwt.secret}. */
    public static final String APP_JWT_SECRET_ENV = "APP_JWT_SECRET";

    /** Profiles in which the shipped placeholder secret is tolerated. */
    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("dev", "test", "local");

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
                      Environment environment) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 characters long for HS256 signing");
        }
        if (DEFAULT_SECRET.equals(secret) && !isDevelopment(environment)) {
            throw new IllegalStateException(
                    "app.jwt.secret is still the built-in development placeholder. Set the "
                    + APP_JWT_SECRET_ENV + " environment variable (at least 32 characters) or "
                    + "activate one of the development profiles " + DEVELOPMENT_PROFILES + ".");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * True when one of the development profiles is active.
     * <p>
     * When no profile is explicitly activated, Spring applies the configured
     * default profile ({@code dev} in {@code application.properties}). The
     * defaults must not keep production in development mode, though: if the
     * operator activates {@code prod}, only the active profiles decide.
     */
    private boolean isDevelopment(Environment environment) {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        String[] profiles = activeProfiles.length > 0
                ? activeProfiles
                : environment.getDefaultProfiles();
        return Arrays.stream(profiles)
                .anyMatch(DEVELOPMENT_PROFILES::contains);
    }

    /** Build a signed token containing the username as subject and the role as a claim. */
    public String generateToken(String username, String role) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    /** Extract the username (subject) from a token, throwing if the token is invalid. */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /** True when the token is well-formed, signed correctly, unexpired and matches {@code username}. */
    public boolean isValid(String token, String username) {
        try {
            Claims claims = parse(token);
            return username != null
                    && username.equals(claims.getSubject())
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
