package com.march.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The JWT secret must be supplied through the environment outside development;
 * shipping with the built-in placeholder is a startup failure.
 */
class JwtServiceTest {

    private static final String CUSTOM_SECRET = "a-custom-production-secret-that-is-long-enough";

    @Test
    void theDefaultSecretIsRejectedOutsideDevelopment() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(JwtService.DEFAULT_SECRET, 1000, profiles("prod")));
    }

    @Test
    void theDefaultSecretIsRejectedWhenNoProfileIsActive() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(JwtService.DEFAULT_SECRET, 1000, profiles()));
    }

    @Test
    void theDefaultSecretIsAllowedInDevelopment() {
        assertDoesNotThrow(() -> new JwtService(JwtService.DEFAULT_SECRET, 1000, profiles("dev")));
        assertDoesNotThrow(() -> new JwtService(JwtService.DEFAULT_SECRET, 1000, profiles("test")));
    }

    @Test
    void theDefaultDevelopmentProfileAllowsThePlaceholder() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("dev");

        assertDoesNotThrow(() -> new JwtService(JwtService.DEFAULT_SECRET, 1000, environment));
    }

    @Test
    void anExplicitProductionProfileOverridesTheDefaultDevelopmentProfile() {
        MockEnvironment environment = profiles("prod");
        environment.setDefaultProfiles("dev");

        assertThrows(IllegalStateException.class,
                () -> new JwtService(JwtService.DEFAULT_SECRET, 1000, environment));
    }

    @Test
    void aCustomSecretIsAllowedEverywhere() {
        assertDoesNotThrow(() -> new JwtService(CUSTOM_SECRET, 1000, profiles("prod")));
    }

    @Test
    void aTooShortSecretIsRejectedEvenInDevelopment() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("short", 1000, profiles("dev")));
    }

    private MockEnvironment profiles(String... active) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(active);
        return environment;
    }
}
