package com.march.backend.controller;

import com.march.backend.dto.AnswerResult;
import com.march.backend.dto.AnswerSubmission;
import com.march.backend.dto.GameResults;
import com.march.backend.dto.GameSummary;
import com.march.backend.dto.RoundResponse;
import com.march.backend.model.Game;
import com.march.backend.model.GameStartRequest;
import com.march.backend.model.Player;
import com.march.backend.service.GameService;
import com.march.backend.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gameplay endpoints: create/join a game or room, fetch the current round,
 * submit answers, advance rounds and read results.
 * <p>
 * The acting player is always derived from the authenticated principal; any
 * {@code playerId} in the request body or query string is ignored. This keeps
 * identity on the server and stops clients from acting as someone else.
 * <p>
 * Every state change is also pushed to WebSocket subscribers (see
 * {@code GameEventPublisher}).
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final PlayerService playerService;

    public GameController(GameService gameService, PlayerService playerService) {
        this.gameService = gameService;
        this.playerService = playerService;
    }

    /** Open games (lobby). */
    @GetMapping
    public List<GameSummary> getOpenGames() {
        return gameService.lobbyGames();
    }

    /** Retrieve a single game; only its participants may read it. */
    @GetMapping("/{id}")
    public Game getGameById(@PathVariable Long id, Authentication authentication) {
        gameService.requireParticipant(id, currentPlayer(authentication).getId());
        return gameService.getById(id);
    }

    /** Create an empty waiting game (rarely needed). */
    @PostMapping
    public Game createGame() {
        return gameService.createGame();
    }

    /** Create a configured solo game and start the first round immediately. */
    @PostMapping("/start")
    public RoundResponse startGame(@RequestBody GameStartRequest request, Authentication authentication) {
        applyCurrentPlayer(request, authentication);
        return gameService.startGame(request);
    }

    /** Create a multiplayer room; it waits until the host starts it. */
    @PostMapping("/create")
    public RoundResponse createRoom(@RequestBody GameStartRequest request, Authentication authentication) {
        applyCurrentPlayer(request, authentication);
        return gameService.createRoom(request);
    }

    /** Host-only: start a waiting room. */
    @PostMapping("/{id}/start")
    public RoundResponse startRoom(@PathVariable Long id, Authentication authentication) {
        return gameService.startRoom(id, currentPlayer(authentication).getId());
    }

    /** Join an existing game before it finishes. */
    @PutMapping("/{id}/join")
    public Game joinGame(@PathVariable Long id, Authentication authentication) {
        return gameService.joinGame(id, currentPlayer(authentication).getId());
    }

    /** Current round (question without the answer) plus the running scores. */
    @GetMapping("/{id}/round")
    public RoundResponse getRound(@PathVariable Long id, Authentication authentication) {
        return gameService.getRound(id, currentPlayer(authentication).getId());
    }

    /** Submit an answer for the current round. */
    @PostMapping("/{id}/answer")
    public AnswerResult submitAnswer(@PathVariable Long id,
                                     @Valid @RequestBody AnswerSubmission submission,
                                     Authentication authentication) {
        return gameService.submitAnswer(
                id,
                currentPlayer(authentication).getId(),
                submission.getQuestionId(),
                submission.getAnswer(),
                submission.getElapsedMs());
    }

    /** Advance to the next round; finishes the game after the last round. */
    @PostMapping("/{id}/next-round")
    public RoundResponse nextRound(@PathVariable Long id, Authentication authentication) {
        return gameService.nextRound(id, currentPlayer(authentication).getId());
    }

    /** Leave the current game; closes the room if it becomes empty. */
    @PostMapping("/{id}/leave")
    public RoundResponse leaveGame(@PathVariable Long id, Authentication authentication) {
        return gameService.leaveGame(id, currentPlayer(authentication).getId());
    }

    /** Host-only: remove another player from the game. */
    @PostMapping("/{id}/kick/{playerId}")
    public RoundResponse kickPlayer(@PathVariable Long id,
                                    @PathVariable Long playerId,
                                    Authentication authentication) {
        return gameService.kickPlayer(id, currentPlayer(authentication).getId(), playerId);
    }

    /** Host-only: cancel (delete) a room that has not started yet. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRoom(@PathVariable Long id, Authentication authentication) {
        gameService.cancelRoom(id, currentPlayer(authentication).getId());
        return ResponseEntity.noContent().build();
    }

    /** Finish a game now (e.g. a player leaves) and return the standings. */
    @PostMapping("/{id}/finish")
    public GameResults finishGame(@PathVariable Long id) {
        return gameService.finishGame(id);
    }

    /** Final standings for a game. */
    @GetMapping("/{id}/results")
    public ResponseEntity<GameResults> getResults(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getResults(id));
    }

    /* ------------------------------------------------------------------ */

    /** Resolve (find or lazily create) the player bound to the authenticated user. */
    private Player currentPlayer(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication required");
        }
        return playerService.resolveForUser(authentication.getName());
    }

    /** Override any client-supplied identity on a start request with the real player. */
    private void applyCurrentPlayer(GameStartRequest request, Authentication authentication) {
        if (request == null) {
            return;
        }
        Player player = currentPlayer(authentication);
        request.setPlayerId(player.getId());
        request.setPlayerName(player.getName());
    }
}
