package com.march.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aggregate statistics for a single player across all finished games. */
@Getter
@Setter
@NoArgsConstructor
public class PlayerStats {

    private Long playerId;
    private String name;
    private int gamesPlayed;
    private int wins;
    private int totalScore;
    private int bestScore;
    private int correctAnswers;
    private int totalRounds;

    /** Correct answers divided by questions seen, as a 0..1 ratio. */
    private double accuracy;
}
