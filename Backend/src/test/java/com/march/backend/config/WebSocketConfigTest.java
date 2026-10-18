package com.march.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * The WebSocket handshake must accept exactly the origins configured for the
 * REST API instead of a wildcard.
 */
class WebSocketConfigTest {

    @Test
    void parsesCommaSeparatedOriginsAndIgnoresBlankEntries() {
        assertArrayEquals(
                new String[]{"http://localhost:4200", "https://quiz.example.com"},
                WebSocketConfig.parseAllowedOrigins(
                        " http://localhost:4200 , https://quiz.example.com , "));
    }

    @Test
    void emptyOrMissingPropertyMeansNoOrigins() {
        assertArrayEquals(new String[0], WebSocketConfig.parseAllowedOrigins(""));
        assertArrayEquals(new String[0], WebSocketConfig.parseAllowedOrigins("   "));
        assertArrayEquals(new String[0], WebSocketConfig.parseAllowedOrigins(null));
    }
}
