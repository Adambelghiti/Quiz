package com.march.backend;

import com.march.backend.dto.DailyChallengeEntry;
import com.march.backend.dto.DailyChallengeStatus;
import com.march.backend.dto.RoundResponse;
import com.march.backend.model.Player;
import com.march.backend.model.Question;
import com.march.backend.repository.DailyChallengeResultRepository;
import com.march.backend.repository.GameHistoryRepository;
import com.march.backend.repository.PlayerRepository;
import com.march.backend.repository.QuestionRepository;
import com.march.backend.service.DailyChallengeService;
import com.march.backend.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The daily challenge is one shared, deterministic set of questions per UTC
 * day. Each player gets one attempt and a separate daily leaderboard.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(DailyChallengeTest.FixedClockConfig.class)
class DailyChallengeTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 19);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(DAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        }
    }

    @Autowired
    private DailyChallengeService dailyService;
    @Autowired
    private GameService gameService;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private GameHistoryRepository gameHistoryRepository;
    @Autowired
    private DailyChallengeResultRepository dailyResultRepository;

    @Test
    void everyPlayerGetsTheSameQuestionsAndAnInProgressAttemptResumes() {
        Player first = newPlayer("daily-first");
        Player second = newPlayer("daily-second");

        RoundResponse firstRound = dailyService.start(first.getId());
        RoundResponse resumed = dailyService.start(first.getId());
        RoundResponse secondRound = dailyService.start(second.getId());

        assertEquals(firstRound.getGameId(), resumed.getGameId(),
                "starting again should resume the attempt instead of creating a second game");
        assertNotEquals(firstRound.getGameId(), secondRound.getGameId(),
                "each player still gets their own game");
        assertEquals(5, firstRound.getTotalRounds());
        assertTrue(firstRound.isDailyChallenge());
        assertFalse(firstRound.isPractice());
        assertEquals(firstRound.getQuestion().getId(), secondRound.getQuestion().getId(),
                "everyone must receive the same first daily question");
        assertEquals("EN", firstRound.getQuestion().getLanguage());
    }

    @Test
    void dailyScoresOnlyAppearOnTheDailyLeaderboard() {
        Player strong = newPlayer("daily-strong");
        Player weak = newPlayer("daily-weak");

        RoundResponse strongRound = dailyService.start(strong.getId());
        Question question = questionRepository.findById(strongRound.getQuestion().getId()).orElseThrow();
        gameService.submitAnswer(strongRound.getGameId(), strong.getId(),
                question.getId(), question.getCorrectAnswer());
        gameService.finishGame(strongRound.getGameId());

        RoundResponse weakRound = dailyService.start(weak.getId());
        gameService.finishGame(weakRound.getGameId());

        DailyChallengeStatus strongStatus = dailyService.status(strong.getId());
        assertTrue(strongStatus.isCompleted());
        assertEquals(strongRound.getGameId(), strongStatus.getGameId());
        assertTrue(strongStatus.getScore() > 0);
        assertEquals(1, strongStatus.getRank());
        assertEquals(2, strongStatus.getLeaderboard().size());

        DailyChallengeEntry top = strongStatus.getLeaderboard().get(0);
        assertEquals(strong.getId(), top.getPlayerId());
        assertEquals(strongStatus.getScore(), top.getScore());

        assertTrue(gameHistoryRepository.findByPlayerId(strong.getId()).isEmpty(),
                "daily attempts must not inflate the all-time leaderboard");
        assertTrue(gameHistoryRepository.findByPlayerId(weak.getId()).isEmpty());
        assertEquals(1, dailyResultRepository.findByChallengeDateAndPlayerId(DAY, strong.getId()).stream().count(),
                "the daily result is stored exactly once");
    }

    @Test
    void aCompletedDailyChallengeCannotBeReplayed() {
        Player player = newPlayer("daily-once");
        RoundResponse round = dailyService.start(player.getId());
        gameService.finishGame(round.getGameId());

        assertThrows(IllegalStateException.class, () -> dailyService.start(player.getId()));
    }

    @Test
    void dailyGamesStayOutOfTheLobbyAndCannotBeJoinedOrRematched() {
        Player player = newPlayer("daily-private");
        Player outsider = newPlayer("daily-outsider");
        RoundResponse round = dailyService.start(player.getId());

        assertFalse(gameService.lobbyGames().stream()
                .anyMatch(g -> g.getId().equals(round.getGameId())));

        assertThrows(IllegalStateException.class,
                () -> gameService.joinGame(round.getGameId(), outsider.getId()));
        gameService.finishGame(round.getGameId());
        assertThrows(IllegalStateException.class,
                () -> gameService.rematch(round.getGameId(), player.getId()));
    }

    private Player newPlayer(String name) {
        Player player = new Player();
        player.setName(name);
        return playerRepository.save(player);
    }
}
