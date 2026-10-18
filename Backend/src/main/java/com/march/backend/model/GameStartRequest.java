package com.march.backend.model;

/**
 * Payload for starting a new game.
 * <p>
 * All filters are optional; a {@code null}/blank value means "any". Unknown
 * values (e.g. a category that does not exist) are treated as "any" rather than
 * causing an error.
 */
public class GameStartRequest {

    private String category;
    private Integer difficulty;
    private String language;

    /** How many questions the game should contain (capped at the available pool). */
    private Integer rounds;

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
