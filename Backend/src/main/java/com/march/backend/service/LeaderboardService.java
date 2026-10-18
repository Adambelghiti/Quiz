package com.march.backend.service;

import java.util.List;
import com.march.backend.model.LeaderboardEntry;

/**
 * Provides leaderboard data aggregated from {@link com.march.backend.model.GameHistory}.
 */
public interface LeaderboardService {
    /**
     * Return the top N players ordered by total score (descending).
     */
    List<LeaderboardEntry> getTopN(int n);
}
