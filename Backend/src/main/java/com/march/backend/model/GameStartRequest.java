package com.march.backend.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Payload for starting a new game.
 * <p>
 * All filters are optional; a {@code null}/blank value means "any". A non-blank
 * value that does not name a real category or language is rejected with a 400
 * response rather than silently being treated as "any".
 */
public class GameStartRequest {

    private String category;
    private Integer difficulty;
    private String language;

    /** How many questions the game should contain (capped at the available pool). */
    private Integer rounds;

    /**
     * Seconds allowed per round. Bounded so a host cannot create a game that is
     * impossible to play or that never advances.
     */
    @Min(value = 10, message = "Round time must be between 10 and 300 seconds")
    @Max(value = 300, message = "Round time must be between 10 and 300 seconds")
    private Integer roundTimeoutSeconds;

    /** Existing player id. When absent/unusable a new player is created. */
    private Long playerId;

    /** Display name used when a new player has to be created. */
    private String playerName;

    public GameStartRequest() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getRounds() {
        return rounds;
    }

    public void setRounds(Integer rounds) {
        this.rounds = rounds;
    }

    public Integer getRoundTimeoutSeconds() {
        return roundTimeoutSeconds;
    }

    public void setRoundTimeoutSeconds(Integer roundTimeoutSeconds) {
        this.roundTimeoutSeconds = roundTimeoutSeconds;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
