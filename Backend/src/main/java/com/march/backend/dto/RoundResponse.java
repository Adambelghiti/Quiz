package com.march.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** State of a game plus the question for the current round. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundResponse {

    private Long gameId;
    private String status;
    private int round;
    private int totalRounds;

    /** The player this response was requested on behalf of, if known. */
    private Long playerId;

    /** The player that hosts the room, if any. */
    private Long hostPlayerId;

    /** Players that have already answered the current round. */
    private List<Long> answeredPlayerIds = new ArrayList<>();

    /** Epoch millis when the current round started; 0 when no round is active. */
    private long roundStartedAt;

    /** Seconds allowed for the current round before it auto-advances. */
    private int roundTimeoutSeconds;

    /** The current question, omitted when the game has finished. */
    private QuestionResponse question;

    private List<PlayerScore> players = new ArrayList<>();

    private boolean finished;

    /** True for an untimed single-player practice session. */
    private boolean practice;

    /** True for the player's single daily-challenge attempt. */
    private boolean dailyChallenge;

    /** ISO date of the daily challenge, when this is one. */
    private String dailyChallengeDate;
}
