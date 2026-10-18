package com.march.backend.service;

import com.march.backend.dto.GameHistoryEntry;
import com.march.backend.dto.PlayerStats;
import com.march.backend.model.Player;
import java.util.List;
import java.util.Optional;

public interface PlayerService {
    List<Player> getAllPlayers();
    Optional<Player> getPlayerById(Long id);
    Player createPlayer(Player player);
    Player updatePlayer(Long id, Player player);
    void deletePlayer(Long id);

    /**
     * Find (or lazily create) the in-game player that belongs to the given
     * authenticated username. This is the single source of truth for identity:
     * clients no longer supply a player id.
     */
    Player resolveForUser(String username);

    /** Finished games for the given user, newest first. */
    List<GameHistoryEntry> historyForUser(String username);

    /** Aggregate statistics for the given user. */
    PlayerStats statsForUser(String username);
}