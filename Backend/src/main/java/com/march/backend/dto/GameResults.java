package com.march.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Final standings of a game. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameResults {

    private Long gameId;
    private String status;
    private Long winnerId;
    private String winnerName;
    private int totalRounds;
    private List<PlayerScore> scores = new ArrayList<>();

    /** True for an untimed single-player practice session. */
    private boolean practice;

    /** True for the player's daily-challenge attempt. */
    private boolean dailyChallenge;

    /** ISO date of the daily challenge, when this is one. */
    private String dailyChallengeDate;
}
