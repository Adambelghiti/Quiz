package com.march.backend.service;

import com.march.backend.dto.DailyChallengeEntry;
import com.march.backend.dto.DailyChallengeStatus;
import com.march.backend.dto.RoundResponse;
import com.march.backend.model.DailyChallenge;
import com.march.backend.model.DailyChallengeResult;
import com.march.backend.model.Game;
import com.march.backend.model.GameStatus;
import com.march.backend.model.Language;
import com.march.backend.repository.DailyChallengeRepository;
import com.march.backend.repository.DailyChallengeResultRepository;
import com.march.backend.repository.GameRepository;
import com.march.backend.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DailyChallengeServiceImpl implements DailyChallengeService {

    private static final int DEFAULT_ROUNDS = 5;
    private static final int DEFAULT_ROUND_TIMEOUT_SECONDS = 60;

    private final DailyChallengeRepository challengeRepository;
    private final DailyChallengeResultRepository resultRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final GameService gameService;
    private final Clock clock;

    public DailyChallengeServiceImpl(DailyChallengeRepository challengeRepository,
                                     DailyChallengeResultRepository resultRepository,
                                     GameRepository gameRepository,
                                     PlayerRepository playerRepository,
                                     GameService gameService,
                                     Clock clock) {
        this.challengeRepository = challengeRepository;
        this.resultRepository = resultRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.gameService = gameService;
        this.clock = clock;
    }

    @Override
    public DailyChallengeStatus status(Long playerId) {
        LocalDate date = LocalDate.now(clock);
        DailyChallenge challenge = challengeRepository.findById(date).orElse(null);

        DailyChallengeStatus status = new DailyChallengeStatus();
        status.setDate(date.toString());
        status.setPlayerId(playerId);
        status.setTotalRounds(challenge != null ? challenge.getTotalRounds() : DEFAULT_ROUNDS);
        status.setRoundTimeoutSeconds(challenge != null
                ? challenge.getRoundTimeoutSeconds()
                : DEFAULT_ROUND_TIMEOUT_SECONDS);
        status.setLanguage((challenge != null ? challenge.getLanguage() : Language.EN).name());

        Game game = gameRepository.findByDailyChallengeKey(key(date, playerId)).orElse(null);
        DailyChallengeResult result = resultRepository
                .findByChallengeDateAndPlayerId(date, playerId)
                .orElse(null);

        if (game != null) {
            status.setGameId(game.getId());
            status.setInProgress(game.getStatus() != GameStatus.FINISHED);
            status.setCompleted(game.getStatus() == GameStatus.FINISHED || result != null);
        }
        if (result != null) {
            status.setCompleted(true);
            status.setInProgress(false);
            status.setScore(result.getScore());
            status.setCorrectAnswers(result.getCorrectAnswers());
        }

        List<DailyChallengeEntry> leaderboard = leaderboard(date);
        status.setLeaderboard(leaderboard);
        leaderboard.stream()
                .filter(entry -> entry.getPlayerId().equals(playerId))
                .findFirst()
                .ifPresent(entry -> status.setRank(entry.getRank()));

        return status;
    }

    @Override
    public RoundResponse start(Long playerId) {
        return gameService.startDailyChallenge(playerId);
    }

    private List<DailyChallengeEntry> leaderboard(LocalDate date) {
        List<DailyChallengeResult> results = resultRepository
                .findByChallengeDateOrderByScoreDescCompletedAtAsc(date);
        List<DailyChallengeEntry> entries = new ArrayList<>();
        int rank = 1;
        for (DailyChallengeResult result : results) {
            String name = playerRepository.findById(result.getPlayerId())
                    .map(player -> player.getName())
                    .orElse("Player " + result.getPlayerId());
            entries.add(new DailyChallengeEntry(
                    rank++,
                    result.getPlayerId(),
                    name,
                    result.getScore(),
                    result.getCorrectAnswers(),
                    result.getTotalRounds()));
        }
        return entries;
    }

    static String key(LocalDate date, Long playerId) {
        return date + ":" + playerId;
    }
}
