package com.march.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Stores the final result of a completed game for historical / leaderboard purposes.
 */
@Entity
@Table(name = "game_history")
@Getter @Setter
public class GameHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long gameId;
    private Long playerId;
    private int finalScore;
    private int rank;
    private int correctAnswers;
    private int totalRounds;
    private LocalDateTime playedAt;

    public GameHistory() { }
}