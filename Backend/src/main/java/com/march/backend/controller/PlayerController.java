package com.march.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import com.march.backend.dto.GameHistoryEntry;
import com.march.backend.dto.PlayerStats;
import com.march.backend.model.Player;
import com.march.backend.service.PlayerService;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    /**
     * The in-game player that belongs to the authenticated user. Resolved (and
     * created on first use) on the server so the client never supplies an id.
     */
    @GetMapping("/me")
    public Player getCurrentPlayer(Authentication authentication) {
        return playerService.resolveForUser(authentication.getName());
    }

    /** Finished games for the authenticated user, newest first. */
    @GetMapping("/me/history")
    public List<GameHistoryEntry> getHistory(Authentication authentication) {
        return playerService.historyForUser(authentication.getName());
    }

    /** Aggregate statistics for the authenticated user. */
    @GetMapping("/me/stats")
    public PlayerStats getStats(Authentication authentication) {
        return playerService.statsForUser(authentication.getName());
    }

    // GET all players
    @GetMapping
    public List<Player> getAllPlayers() {
        return playerService.getAllPlayers();
    }

    // GET a specific player by ID
    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable Long id) {
        return playerService.getPlayerById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST - Create a new player
    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
        return ResponseEntity.ok(playerService.createPlayer(player));
    }

    // PUT - Update an existing player
    @PutMapping("/{id}")
    public ResponseEntity<Player> updatePlayer(@PathVariable Long id, @RequestBody Player playerDetails) {
        return playerService.getPlayerById(id)
                .map(existing -> {
                    existing.setName(playerDetails.getName());
                    existing.setAvatar(playerDetails.getAvatar());
                    return ResponseEntity.ok(playerService.updatePlayer(id, existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // DELETE a player
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        if (playerService.getPlayerById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
