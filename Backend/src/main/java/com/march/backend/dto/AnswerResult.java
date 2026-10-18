package com.march.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Outcome of a single answer submission. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResult {

    private boolean correct;
    private String correctAnswer;
    private int scoreDelta;
    private int totalScore;
    private int correctAnswers;
    private int round;
    private int totalRounds;

    /** True once every player in the game has answered the current round. */
    private boolean roundComplete;
}
