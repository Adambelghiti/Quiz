package com.march.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Today's daily challenge, the caller's attempt state, and the leaderboard. */
@Getter
@Setter
@NoArgsConstructor
public class DailyChallengeStatus {

    /** ISO date (UTC) of the challenge. */
    private String date;

    private Long playerId;

    private int totalRounds;

    private int roundTimeoutSeconds;

    private String language;

    /** True when the caller has an unfinished attempt that can be resumed. */
    private boolean inProgress;

    /** True when the caller's attempt has been completed. */
    private boolean completed;

    /** The caller's daily-challenge game, when one exists. */
    private Long gameId;

    private Integer score;

    private Integer correctAnswers;

    /** 1-based rank on today's leaderboard, when completed. */
    private Integer rank;

    private List<DailyChallengeEntry> leaderboard = new ArrayList<>();
}
