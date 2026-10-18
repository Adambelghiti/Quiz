package com.march.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One finished game in a player's history. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameHistoryEntry {

    private Long gameId;
    private int finalScore;
    private int rank;
    private int correctAnswers;
    private int totalRounds;
    private LocalDateTime playedAt;
}
