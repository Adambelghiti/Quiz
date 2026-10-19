package com.march.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of the daily-challenge leaderboard. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyChallengeEntry {

    private int rank;
    private Long playerId;
    private String name;
    private int score;
    private int correctAnswers;
    private int totalRounds;
}
