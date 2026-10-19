package com.march.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Final score of one player's daily attempt. Kept separate from
 * {@link GameHistory} so daily results do not inflate the all-time leaderboard.
 */
@Entity
@Table(name = "daily_challenge_results", uniqueConstraints =
        @UniqueConstraint(name = "uk_daily_result_player",
                columnNames = {"challenge_date", "player_id"}))
@Getter
@Setter
@NoArgsConstructor
public class DailyChallengeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate challengeDate;

    private Long playerId;

    private Long gameId;

    private int score;

    private int correctAnswers;

    private int totalRounds;

    private LocalDateTime completedAt;
}
