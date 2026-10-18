package com.march.backend.service;

import com.march.backend.model.GameHistory;
import com.march.backend.model.LeaderboardEntry;
import com.march.backend.repository.GameHistoryRepository;
import com.march.backend.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates finished-game history into a leaderboard of total scores per player.
 */
@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final GameHistoryRepository gameHistoryRepository;
    private final PlayerRepository playerRepository;

    public LeaderboardServiceImpl(GameHistoryRepository gameHistoryRepository,
                                  PlayerRepository playerRepository) {
        this.gameHistoryRepository = gameHistoryRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public List<LeaderboardEntry> getTopN(int n) {
        Map<Long, int[]> aggregate = new HashMap<>(); // [totalScore, games, wins, correct, rounds]
        for (GameHistory history : gameHistoryRepository.findAll()) {
            int[] agg = aggregate.computeIfAbsent(history.getPlayerId(), k -> new int[5]);
            agg[0] += history.getFinalScore();
            agg[1] += 1;
            if (history.getRank() == 1) {
                agg[2] += 1;
            }
            agg[3] += history.getCorrectAnswers();
            agg[4] += history.getTotalRounds();
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, int[]> entry : aggregate.entrySet()) {
            int[] agg = entry.getValue();
            String name = playerRepository.findById(entry.getKey())
                    .map(player -> player.getName())
                    .orElse("Player " + entry.getKey());
            LeaderboardEntry leaderboardEntry = new LeaderboardEntry(entry.getKey(), name, agg[0]);
            leaderboardEntry.setGamesPlayed(agg[1]);
            leaderboardEntry.setWins(agg[2]);
            leaderboardEntry.setCorrectAnswers(agg[3]);
            leaderboardEntry.setTotalRounds(agg[4]);
            leaderboardEntry.setAccuracy(agg[4] > 0 ? (double) agg[3] / agg[4] : 0.0);
            entries.add(leaderboardEntry);
        }

        entries.sort(Comparator.comparingInt(LeaderboardEntry::getTotalScore).reversed());
        int limit = Math.max(0, Math.min(n, entries.size()));
        return entries.subList(0, limit);
    }
}
