package com.march.backend.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LeaderboardEntry {
    private Long playerId;
    private String name;
    private int totalScore;
    private int gamesPlayed;
    private int wins;
    private int correctAnswers;
    private int totalRounds;

    /** Correct answers divided by questions seen, as a 0..1 ratio. */
    private double accuracy;

    public LeaderboardEntry(Long playerId, String name, int totalScore) {
        this.playerId = playerId;
        this.name = name;
        this.totalScore = totalScore;
    }
}