package com.march.backend.service;

/**
 * Service responsible for calculating points based on difficulty and time taken.
 */
public interface ScoringService {
    /**
     * Calculate score for a single answer.
     * @param basePoints Base points awarded before multipliers/penalties.
     * @param difficulty Difficulty level of the question (1–3).
     * @param elapsedMs Time taken to answer in milliseconds.
     * @return Calculated score, never negative.
     */
    int calculateScore(int basePoints, int difficulty, long elapsedMs);
}