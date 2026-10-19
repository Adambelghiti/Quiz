package com.march.backend;

import com.march.backend.dto.AnswerResult;
import com.march.backend.dto.RoundResponse;
import com.march.backend.model.Game;
import com.march.backend.model.GameStartRequest;
import com.march.backend.model.Player;
import com.march.backend.model.Question;
import com.march.backend.repository.GameHistoryRepository;
import com.march.backend.repository.GameRepository;
import com.march.backend.repository.PlayerRepository;
import com.march.backend.repository.QuestionRepository;
import com.march.backend.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Practice games are single-player and untimed. They are for learning, so they
 * must not appear in the lobby or add rows to the competitive leaderboard.
 */
@SpringBootTest
@ActiveProfiles("test")
class PracticeModeTest {

    @Autowired
    private GameService gameService;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private GameHistoryRepository gameHistoryRepository;
    @Autowired
    private GameRepository gameRepository;

    @Test
    void practiceIsUntimedAndDoesNotRecordHistory() {
        Player player = newPlayer("practice-player");

        GameStartRequest request = new GameStartRequest();
        request.setPlayerId(player.getId());
        request.setPractice(true);
        request.setDifficulty(1);
        request.setRounds(1);
        // The setup screen still sends a normal time, but practice ignores it.
        request.setRoundTimeoutSeconds(15);

        RoundResponse round = gameService.startGame(request);

        assertTrue(round.isPractice(), "the client must know this is a practice game");
        assertEquals(0, round.getRoundTimeoutSeconds(), "practice questions have no deadline");
        assertFalse(gameService.lobbyGames().stream()
                .anyMatch(g -> g.getId().equals(round.getGameId())),
                "practice games must not be advertised as joinable rooms");

        // Make the round look hours old. An untimed practice answer is still on time.
        Game game = gameRepository.findById(round.getGameId()).orElseThrow();
        game.setRoundStartedAt(System.currentTimeMillis() - 3_600_000L);
        gameRepository.save(game);

        Question question = questionRepository.findById(round.getQuestion().getId()).orElseThrow();
        AnswerResult result = gameService.submitAnswer(
                round.getGameId(), player.getId(), question.getId(), question.getCorrectAnswer());

        assertTrue(result.isCorrect());
        assertFalse(result.isLate(), "an untimed practice answer is never late");
        assertTrue(result.getScoreDelta() > 0, "practice still gives positive feedback");

        assertTrue(gameService.nextRound(round.getGameId(), player.getId()).isFinished());
        assertTrue(gameHistoryRepository.findByPlayerId(player.getId()).isEmpty(),
                "practice must not affect the competitive leaderboard");
    }

    private Player newPlayer(String name) {
        Player player = new Player();
        player.setName(name);
        return playerRepository.save(player);
    }
}
