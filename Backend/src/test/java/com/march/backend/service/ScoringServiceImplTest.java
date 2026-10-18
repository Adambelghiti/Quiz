package com.march.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringServiceImplTest {

    private final ScoringServiceImpl scoring = new ScoringServiceImpl();

    @Test
    void difficultyMultipliesBasePoints() {
        assertEquals(100, scoring.calculateScore(100, 1, 0));
        assertEquals(150, scoring.calculateScore(100, 2, 0));
        assertEquals(200, scoring.calculateScore(100, 3, 0));
    }

    @Test
    void timePenaltyReducesScore() {
        // 100 base, difficulty 1, 5 seconds -> 100 - 5
        assertEquals(95, scoring.calculateScore(100, 1, 5000));
    }

    @Test
    void scoreNeverGoesNegative() {
        assertEquals(0, scoring.calculateScore(100, 1, 600_000));
    }
}
