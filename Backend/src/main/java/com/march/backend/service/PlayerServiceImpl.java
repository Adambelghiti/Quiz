package com.march.backend.service;

import com.march.backend.dto.GameHistoryEntry;
import com.march.backend.dto.PlayerStats;
import com.march.backend.model.AuthUser;
import com.march.backend.model.GameHistory;
import com.march.backend.model.Player;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.repository.GameHistoryRepository;
import com.march.backend.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final AuthUserRepository authUserRepository;
    private final GameHistoryRepository gameHistoryRepository;

    public PlayerServiceImpl(PlayerRepository playerRepository,
                            AuthUserRepository authUserRepository,
                            GameHistoryRepository gameHistoryRepository) {
        this.playerRepository = playerRepository;
        this.authUserRepository = authUserRepository;
        this.gameHistoryRepository = gameHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findById(id);
    }

    @Override
    public Player createPlayer(Player player) {
        return playerRepository.save(player);
    }

    @Override
    public Player updatePlayer(Long id, Player player) {
        // Ensure the target exists before persisting
        if (!playerRepository.existsById(id)) {
            throw new EntityNotFoundException("Player not found: " + id);
        }
        player.setId(id);
        return playerRepository.save(player);
    }

    @Override
    public void deletePlayer(Long id) {
        playerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Player> findForUser(String username) {
        AuthUser user = authUserRepository.findByUsername(username);
        if (user == null) {
            return Optional.empty();
        }
        return playerRepository.findByAuthUserId(user.getId());
    }

    @Override
    public Player resolveForUser(String username) {
        return findForUser(username).orElseGet(() -> {
            AuthUser user = authUserRepository.findByUsername(username);
            if (user == null) {
                throw new EntityNotFoundException("User not found: " + username);
            }
            Player player = new Player();
            player.setName(user.getUsername());
            player.setAuthUserId(user.getId());
            return playerRepository.save(player);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameHistoryEntry> historyForUser(String username) {
        Player player = resolveForUser(username);
        return gameHistoryRepository.findByPlayerIdOrderByPlayedAtDesc(player.getId()).stream()
                .map(this::toEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerStats statsForUser(String username) {
        Player player = resolveForUser(username);
        List<GameHistory> history = gameHistoryRepository.findByPlayerId(player.getId());

        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(player.getId());
        stats.setName(player.getName());
        stats.setGamesPlayed(history.size());

        int totalScore = 0;
        int bestScore = 0;
        int wins = 0;
        int correct = 0;
        int rounds = 0;
        for (GameHistory entry : history) {
            totalScore += entry.getFinalScore();
            bestScore = Math.max(bestScore, entry.getFinalScore());
            if (entry.getRank() == 1) {
                wins++;
            }
            correct += entry.getCorrectAnswers();
            rounds += entry.getTotalRounds();
        }
        stats.setTotalScore(totalScore);
        stats.setBestScore(bestScore);
        stats.setWins(wins);
        stats.setCorrectAnswers(correct);
        stats.setTotalRounds(rounds);
        stats.setAccuracy(rounds > 0 ? (double) correct / rounds : 0.0);
        return stats;
    }

    private GameHistoryEntry toEntry(GameHistory history) {
        return new GameHistoryEntry(
                history.getGameId(),
                history.getFinalScore(),
                history.getRank(),
                history.getCorrectAnswers(),
                history.getTotalRounds(),
                history.getPlayedAt());
    }
}
