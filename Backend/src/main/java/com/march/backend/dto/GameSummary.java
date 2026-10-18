package com.march.backend.dto;

import com.march.backend.model.Game;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lightweight game representation for listing games in the lobby. */
@Getter
@Setter
@NoArgsConstructor
public class GameSummary {

    private Long id;
    private String status;
    private int currentRound;
    private int totalRounds;
    private int playerCount;

    public static GameSummary from(Game game) {
        GameSummary summary = new GameSummary();
        summary.id = game.getId();
        summary.status = game.getStatus().name();
        summary.currentRound = game.getCurrentRound();
        summary.totalRounds = game.getTotalRounds();
        summary.playerCount = game.getPlayers() != null ? game.getPlayers().size() : 0;
        return summary;
    }
}
