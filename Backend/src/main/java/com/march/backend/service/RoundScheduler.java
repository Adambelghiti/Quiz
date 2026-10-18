package com.march.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives time-based game transitions: rounds that run past their timeout are
 * advanced automatically, and waiting rooms that are never started are cleaned
 * up. Runs on a short fixed delay so multiplayer games cannot hang.
 */
@Component
public class RoundScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoundScheduler.class);

    private final GameService gameService;

    public RoundScheduler(GameService gameService) {
        this.gameService = gameService;
    }

    /** Auto-advance expired rounds. */
    @Scheduled(fixedDelay = 2000L, initialDelay = 5000L)
    public void advanceExpiredRounds() {
        try {
            gameService.autoAdvanceExpiredGames();
        } catch (Exception ex) {
            log.warn("Failed to auto-advance expired rounds: {}", ex.getMessage());
        }
    }

    /** Remove waiting rooms that were never started. */
    @Scheduled(fixedDelay = 60000L, initialDelay = 60000L)
    public void expireWaitingRooms() {
        try {
            gameService.expireStaleWaitingRooms();
        } catch (Exception ex) {
            log.warn("Failed to expire waiting rooms: {}", ex.getMessage());
        }
    }
}
