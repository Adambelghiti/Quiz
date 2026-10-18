package com.march.backend.service;

import org.springframework.stereotype.Service;

/**
 * Simple scoring algorithm.
 * <p>
 * Base points are multiplied by a difficulty multiplier (1 → 1x, 2 → 1.5x, 3 → 2x).
 * A time penalty of 1 point per second is subtracted, ensuring the final score
 * never falls below zero.
 */
@Service
public class ScoringServiceImpl implements ScoringService {

    @Override
    public int calculateScore(int basePoints, int difficulty, long elapsedMs) {
        double multiplier = switch (difficulty) {
            case 1 -> 1.0;
            case 2 -> 1.5;
            case 3 -> 2.0;
            default -> 1.0; // fallback
        };
        int rawScore = (int) Math.round(basePoints * multiplier);
        // Penalize 1 point per second, cap at zero.
        int penalty = (int) (elapsedMs / 1000L);
        return Math.max(rawScore - penalty, 0);
    }
}
