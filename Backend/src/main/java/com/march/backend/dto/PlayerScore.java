package com.march.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A player's running totals inside a game. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerScore {

    private Long playerId;
    private String name;
    private int score;
    private int correctAnswers;
}
