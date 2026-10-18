package com.march.backend.service;

import com.march.backend.dto.AnswerResult;
import com.march.backend.dto.GameResults;
import com.march.backend.dto.GameSummary;
import com.march.backend.dto.RoundResponse;
import com.march.backend.model.Game;
import com.march.backend.model.GameStartRequest;

import java.util.List;

public interface GameService {

    /** Create an empty game in the {@code WAITING} state. */
    Game createGame();

    /** Create and immediately start a solo game, returning its first round. */
    RoundResponse startGame(GameStartRequest request);

    /** Create a multiplayer room in the {@code WAITING} state; the host starts it later. */
    RoundResponse createRoom(GameStartRequest request);

    /** Host-only: start a waiting room and deal the first question. */
    RoundResponse startRoom(Long gameId, Long playerId);

    /** Add a player to a game that has not finished yet. */
    Game joinGame(Long gameId, Long playerId);

    /** Remove the current player from a game; closes the room if it becomes empty. */
    RoundResponse leaveGame(Long gameId, Long playerId);

    /** Host-only: remove another player from the game. */
    RoundResponse kickPlayer(Long gameId, Long hostPlayerId, Long targetPlayerId);

    /** Host-only: delete a room that has not started yet. */
    void cancelRoom(Long gameId, Long playerId);

    /** Current state of a game plus the question for the active round. */
    RoundResponse getRound(Long gameId, Long playerId);

    /** Submit an answer for the current round and return the outcome. */
    AnswerResult submitAnswer(Long gameId, Long playerId, Long questionId, String answer, long elapsedMs);

    /**
     * Advance to the next round. Allowed once every player has answered, or at
     * any time for the host (who can force the game forward).
     */
    RoundResponse nextRound(Long gameId, Long playerId);

    /** Finish a game and return its final standings. */
    GameResults finishGame(Long gameId);

    /** Final standings for a game. */
    GameResults getResults(Long gameId);

    /** Open games (not finished) for the lobby. */
    List<GameSummary> lobbyGames();

    /** Advance every in-progress game whose round has exceeded its timeout. */
    void autoAdvanceExpiredGames();

    /** Delete waiting rooms that have been idle beyond their TTL. */
    void expireStaleWaitingRooms();

    /** Find a game by id or throw {@code EntityNotFoundException}. */
    Game getById(Long id);

    /**
     * Ensure the given player is part of the game; throws
     * {@code AccessDeniedException} otherwise. Used to scope reads to players
     * who are actually in the game.
     */
    void requireParticipant(Long gameId, Long playerId);
}
