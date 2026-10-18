package com.march.backend;

import com.march.backend.dto.AnswerResult;
import com.march.backend.dto.GameResults;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end exercise of the game lifecycle against the real services and an
 * in-memory database.
 */
@SpringBootTest
@ActiveProfiles("test")
class GameFlowTest {

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
    void soloGameRunsToCompletionAndRecordsHistory() {
        Player player = newPlayer("solo-tester");

        GameStartRequest request = new GameStartRequest();
        request.setPlayerId(player.getId());
        request.setDifficulty(1);
        request.setRounds(3);

        RoundResponse round = gameService.startGame(request);
        assertNotNull(round.getQuestion(), "the first round should deal a question");
        assertTrue(round.getRoundStartedAt() > 0, "the round start time should be set");

        int guard = 0;
        while (!round.isFinished() && guard++ < 20) {
            Question question = questionRepository.findById(round.getQuestion().getId()).orElseThrow();
            AnswerResult result = gameService.submitAnswer(
                    round.getGameId(), player.getId(), question.getId(), question.getCorrectAnswer());
            assertTrue(result.isCorrect(), "submitting the stored answer should be correct");
            round = gameService.nextRound(round.getGameId(), player.getId());
        }

        assertTrue(round.isFinished(), "the game should finish after its rounds");
        assertTrue(guard <= 3, "a 3-round game should need at most 3 submissions");

        GameResults results = gameService.getResults(round.getGameId());
        assertEquals(1, results.getScores().size());
        assertEquals(player.getId(), results.getScores().get(0).getPlayerId());
        assertTrue(results.getScores().get(0).getScore() > 0, "correct answers should score points");

        assertFalse(gameHistoryRepository.findByPlayerId(player.getId()).isEmpty(),
                "finishing a game should record history");
    }

    @Test
    void aZeroScorePlayerStillGetsHistory() {
        Player player = newPlayer("zero-score-tester");

        GameStartRequest request = new GameStartRequest();
        request.setPlayerId(player.getId());
        request.setDifficulty(1);
        request.setRounds(1);

        RoundResponse round = gameService.startGame(request);
        gameService.submitAnswer(round.getGameId(), player.getId(),
                round.getQuestion().getId(), "not-the-answer");

        RoundResponse afterRound = gameService.nextRound(round.getGameId(), player.getId());
        assertTrue(afterRound.isFinished());

        var history = gameHistoryRepository.findByPlayerId(player.getId());
        assertFalse(history.isEmpty(), "even a zero-score player must appear in history");
        assertEquals(0, history.get(0).getFinalScore());
    }

    @Test
    void aLateCorrectAnswerScoresNothing() {
        Player player = newPlayer("late-answerer");

        GameStartRequest request = new GameStartRequest();
        request.setPlayerId(player.getId());
        request.setDifficulty(1);
        request.setRounds(1);

        RoundResponse round = gameService.startGame(request);
        Question question = questionRepository.findById(round.getQuestion().getId()).orElseThrow();

        // Push the round start into the past so the answer arrives after the
        // 30 second deadline (as if it were submitted ~40s into the round).
        Game game = gameRepository.findById(round.getGameId()).orElseThrow();
        game.setRoundStartedAt(System.currentTimeMillis() - 40_000L);
        gameRepository.save(game);

        AnswerResult result = gameService.submitAnswer(
                round.getGameId(), player.getId(), question.getId(), question.getCorrectAnswer());

        assertTrue(result.isCorrect(), "the submitted answer is still the correct one");
        assertEquals(0, result.getScoreDelta(), "a late answer must not award points");
        assertEquals(0, result.getTotalScore(), "a late answer must not change the total score");
        assertEquals(0, result.getCorrectAnswers(), "a late answer must not count as correct");
    }

    @Test
    void leavingReassignsHostAndRemovesPlayer() {
        Player host = newPlayer("host-tester");
        Player guest = newPlayer("guest-tester");

        GameStartRequest request = new GameStartRequest();
        request.setPlayerId(host.getId());
        request.setDifficulty(1);
        request.setRounds(2);

        RoundResponse room = gameService.createRoom(request);
        assertEquals(host.getId(), room.getHostPlayerId());

        gameService.joinGame(room.getGameId(), guest.getId());

        RoundResponse afterLeave = gameService.leaveGame(room.getGameId(), host.getId());
        assertEquals(1, afterLeave.getPlayers().size(), "the leaving player should be removed");
        assertEquals(guest.getId(), afterLeave.getHostPlayerId(), "host should pass to the remaining player");
    }

    @Test
    void rematchReusesTheRoomAndResetsScoresForSolo() {
        Player player = newPlayer("rematch-solo");

        GameStartRequest request = new GameStartRequest();
        request.setPlayerId(player.getId());
        request.setDifficulty(1);
        request.setRounds(1);

        RoundResponse round = gameService.startGame(request);
        Question question = questionRepository.findById(round.getQuestion().getId()).orElseThrow();
        gameService.submitAnswer(round.getGameId(), player.getId(), question.getId(), question.getCorrectAnswer());
        assertTrue(gameService.nextRound(round.getGameId(), player.getId()).isFinished());

        RoundResponse rematch = gameService.rematch(round.getGameId(), player.getId());

        assertEquals(round.getGameId(), rematch.getGameId(), "a rematch must reuse the same room");
        assertEquals("IN_PROGRESS", rematch.getStatus(), "a solo rematch restarts immediately");
        assertEquals(1, rematch.getRound(), "the rematch starts again at round 1");
        assertNotNull(rematch.getQuestion(), "the rematch deals a fresh question");
        assertEquals(1, rematch.getPlayers().size());
        assertEquals(0, rematch.getPlayers().get(0).getScore(), "the previous scores are cleared");
    }

    @Test
    void rematchKeepsThePlayersAndWaitsForTheHost() {
        Player host = newPlayer("rematch-host");
        Player guest = newPlayer("rematch-guest");
        Player outsider = newPlayer("rematch-outsider");

        GameStartRequest request = new GameStartRequest();
        request.setPlayerId(host.getId());
        request.setDifficulty(1);
        request.setRounds(2);

        RoundResponse room = gameService.createRoom(request);
        gameService.joinGame(room.getGameId(), guest.getId());
        gameService.startRoom(room.getGameId(), host.getId());
        assertEquals("FINISHED", gameService.finishGame(room.getGameId()).getStatus());

        assertThrows(AccessDeniedException.class,
                () -> gameService.rematch(room.getGameId(), outsider.getId()),
                "only participants may rematch");

        RoundResponse rematch = gameService.rematch(room.getGameId(), guest.getId());
        assertEquals("WAITING", rematch.getStatus(), "a multiplayer rematch waits for the host");
        assertEquals(room.getGameId(), rematch.getGameId());
        assertEquals(2, rematch.getPlayers().size(), "every player is kept in the room");
        assertEquals(host.getId(), rematch.getHostPlayerId(), "the host is unchanged");
        assertTrue(rematch.getPlayers().stream().allMatch(p -> p.getScore() == 0), "scores are reset");

        // Calling it again while the room is already waiting changes nothing.
        RoundResponse again = gameService.rematch(room.getGameId(), host.getId());
        assertEquals("WAITING", again.getStatus());
        assertEquals(2, again.getPlayers().size());
    }

    private Player newPlayer(String name) {
        Player player = new Player();
        player.setName(name);
        return playerRepository.save(player);
    }
}
