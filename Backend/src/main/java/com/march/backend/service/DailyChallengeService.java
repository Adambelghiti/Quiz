package com.march.backend.service;

import com.march.backend.dto.DailyChallengeStatus;
import com.march.backend.dto.RoundResponse;

public interface DailyChallengeService {

    /** Today's challenge state for the given player, including the leaderboard. */
    DailyChallengeStatus status(Long playerId);

    /** Start or resume the player's attempt for today. */
    RoundResponse start(Long playerId);
}
