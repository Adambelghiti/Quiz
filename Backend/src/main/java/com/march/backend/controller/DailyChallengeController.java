package com.march.backend.controller;

import com.march.backend.dto.DailyChallengeStatus;
import com.march.backend.dto.RoundResponse;
import com.march.backend.service.DailyChallengeService;
import com.march.backend.service.PlayerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The once-per-day challenge. The caller's player identity always comes from
 * the authenticated principal, so a client cannot submit for someone else.
 */
@RestController
@RequestMapping("/api/daily")
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;
    private final PlayerService playerService;

    public DailyChallengeController(DailyChallengeService dailyChallengeService,
                                    PlayerService playerService) {
        this.dailyChallengeService = dailyChallengeService;
        this.playerService = playerService;
    }

    /** Today's challenge state, the caller's attempt, and the daily leaderboard. */
    @GetMapping
    public DailyChallengeStatus getStatus(Authentication authentication) {
        Long playerId = playerService.resolveForUser(authentication.getName()).getId();
        return dailyChallengeService.status(playerId);
    }

    /** Start a fresh attempt, or resume an unfinished one (one attempt per day). */
    @PostMapping("/start")
    public RoundResponse start(Authentication authentication) {
        Long playerId = playerService.resolveForUser(authentication.getName()).getId();
        return dailyChallengeService.start(playerId);
    }
}
