package com.march.backend.service;

import com.march.backend.dto.AnswerResult;
import com.march.backend.dto.GameResults;
import com.march.backend.dto.GameSummary;
import com.march.backend.dto.PlayerScore;
import com.march.backend.dto.QuestionResponse;
import com.march.backend.dto.RoundResponse;
import com.march.backend.model.Category;
import com.march.backend.model.Game;
import com.march.backend.model.GameHistory;
import com.march.backend.model.GameStartRequest;
import com.march.backend.model.GameStatus;
import com.march.backend.model.Language;
import com.march.backend.model.Player;
import com.march.backend.model.Question;
import com.march.backend.repository.GameHistoryRepository;
import com.march.backend.repository.GameRepository;
import com.march.backend.repository.PlayerRepository;
import com.march.backend.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
@Transactional
public class GameServiceImpl implements GameService {

    /** Points for a correct answer at difficulty 1 (before multiplier / time penalty). */
    private static final int BASE_POINTS = 100;

    private static final int DEFAULT_ROUNDS = 5;

    /** Waiting rooms with no activity for this long are cleaned up. */
    private static final long WAITING_ROOM_TTL_MS = 30L * 60L * 1000L;

    private final GameRepository gameRepo;
    private final PlayerRepository playerRepo;
    private final QuestionRepository questionRepo;
    private final GameHistoryRepository gameHistoryRepo;
    private final ScoringService scoringService;
    private final GameEventPublisher publisher;
    private final Random random = new Random();

    public GameServiceImpl(GameRepository gameRepo,
                           PlayerRepository playerRepo,
                           QuestionRepository questionRepo,
                           GameHistoryRepository gameHistoryRepo,
                           ScoringService scoringService,
                           GameEventPublisher publisher) {
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
        this.questionRepo = questionRepo;
        this.gameHistoryRepo = gameHistoryRepo;
        this.scoringService = scoringService;
        this.publisher = publisher;
    }

    @Override
    public Game createGame() {
        Game game = new Game();
        game.setStatus(GameStatus.WAITING);
        return gameRepo.save(game);
    }

    @Override
    public RoundResponse startGame(GameStartRequest request) {
        Player player = resolvePlayer(request.getPlayerId(), request.getPlayerName());
        Game game = newGame(request, player);
        game.setStatus(GameStatus.IN_PROGRESS);
        gameRepo.save(game);

        startRound(game);
        gameRepo.save(game);

        broadcast(game);
        return buildRound(game, player.getId());
    }

    @Override
    public RoundResponse createRoom(GameStartRequest request) {
        Player player = resolvePlayer(request.getPlayerId(), request.getPlayerName());
        Game game = newGame(request, player);
        game.setStatus(GameStatus.WAITING);
        gameRepo.save(game);

        broadcast(game);
        return buildRound(game, player.getId());
    }

    @Override
    public RoundResponse startRoom(Long gameId, Long playerId) {
        Game game = getForUpdate(gameId);
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("This game has already started");
        }
        if (playerId == null || !playerId.equals(game.getHostPlayerId())) {
            throw new IllegalStateException("Only the host can start the game");
        }
        game.setStatus(GameStatus.IN_PROGRESS);
        startRound(game);
        gameRepo.save(game);

        broadcast(game);
        return buildRound(game, playerId);
    }

    @Override
    public Game joinGame(Long gameId, Long playerId) {
        Game game = getForUpdate(gameId);
        if (game.getStatus() == GameStatus.FINISHED) {
            throw new IllegalStateException("Cannot join a game that has already finished");
        }
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player not found: " + playerId));
        boolean alreadyIn = game.getPlayers().stream()
                .anyMatch(p -> p.getId().equals(playerId));
        if (!alreadyIn) {
            game.getPlayers().add(player);
        }
        Game saved = gameRepo.save(game);

        broadcast(saved);
        return saved;
    }

    @Override
    public RoundResponse leaveGame(Long gameId, Long playerId) {
        Game game = getForUpdate(gameId);
        requirePlayer(game, playerId);

        removePlayer(game, playerId);

        if (game.getPlayers().isEmpty()) {
            // No one left in the room: close it without recording a history entry.
            game.setStatus(GameStatus.FINISHED);
            gameRepo.save(game);
        } else {
            if (playerId.equals(game.getHostPlayerId())) {
                game.setHostPlayerId(game.getPlayers().get(0).getId());
            }
            gameRepo.save(game);
        }
        broadcast(game);
        return buildRound(game, playerId);
    }

    @Override
    public RoundResponse kickPlayer(Long gameId, Long hostPlayerId, Long targetPlayerId) {
        Game game = getForUpdate(gameId);
        if (hostPlayerId == null || !hostPlayerId.equals(game.getHostPlayerId())) {
            throw new IllegalStateException("Only the host can remove players");
        }
        if (targetPlayerId == null || targetPlayerId.equals(hostPlayerId)) {
            throw new IllegalStateException("The host cannot remove themselves");
        }
        requirePlayer(game, targetPlayerId);

        removePlayer(game, targetPlayerId);
        gameRepo.save(game);

        broadcast(game);
        return buildRound(game, hostPlayerId);
    }

    @Override
    public void cancelRoom(Long gameId, Long playerId) {
        Game game = getForUpdate(gameId);
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("Only a game that has not started can be cancelled");
        }
        if (playerId == null || !playerId.equals(game.getHostPlayerId())) {
            throw new IllegalStateException("Only the host can cancel the room");
        }
        gameRepo.delete(game);
        publisher.publishLobby(lobbyGames());
    }

    @Override
    public RoundResponse getRound(Long gameId, Long playerId) {
        Game game = getById(gameId);
        if (playerId == null || game.getPlayers().stream().noneMatch(p -> p.getId().equals(playerId))) {
            throw new AccessDeniedException("You are not a player in this game");
        }
        return buildRound(game, playerId);
    }

    @Override
    public AnswerResult submitAnswer(Long gameId, Long playerId, Long questionId, String answer, long elapsedMs) {
        Game game = getForUpdate(gameId);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }
        if (game.getCurrentQuestionId() == null || !game.getCurrentQuestionId().equals(questionId)) {
            throw new IllegalStateException("That is not the question for the current round");
        }
        requirePlayer(game, playerId);
        if (game.getAnsweredPlayerIds().contains(playerId)) {
            throw new IllegalStateException("You have already answered this round");
        }

        Question question = questionRepo.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Question not found: " + questionId));

        boolean correct = isCorrect(question.getCorrectAnswer(), answer);
        int delta = 0;
        if (correct) {
            // Time is measured on the server from when the round started; the
            // client-supplied value is ignored so it cannot be gamed.
            delta = scoringService.calculateScore(BASE_POINTS, question.getDifficulty(), serverElapsedMs(game));
            game.getScores().merge(playerId, delta, Integer::sum);
            game.getCorrectAnswers().merge(playerId, 1, Integer::sum);
        }
        game.getAnsweredPlayerIds().add(playerId);
        gameRepo.save(game);

        // Let the other players see the updated scoreboard (and who has answered).
        publisher.publishGame(gameId, buildRound(game, null));

        AnswerResult result = new AnswerResult();
        result.setCorrect(correct);
        result.setCorrectAnswer(question.getCorrectAnswer());
        result.setScoreDelta(delta);
        result.setTotalScore(game.getScores().getOrDefault(playerId, 0));
        result.setCorrectAnswers(game.getCorrectAnswers().getOrDefault(playerId, 0));
        result.setRound(game.getCurrentRound());
        result.setTotalRounds(game.getTotalRounds());
        result.setRoundComplete(game.getAnsweredPlayerIds().containsAll(playerIds(game)));
        result.setFinished(game.getStatus() == GameStatus.FINISHED);
        return result;
    }

    @Override
    public RoundResponse nextRound(Long gameId, Long playerId) {
        Game game = getForUpdate(gameId);
        if (game.getStatus() == GameStatus.FINISHED) {
            return buildRound(game, playerId);
        }

        boolean allAnswered = game.getAnsweredPlayerIds().containsAll(playerIds(game));
        boolean isHost = playerId != null && playerId.equals(game.getHostPlayerId());
        if (!allAnswered && !isHost) {
            throw new IllegalStateException("Waiting for all players to answer");
        }

        return advanceRound(game, playerId);
    }

    @Override
    public GameResults finishGame(Long gameId) {
        Game game = getForUpdate(gameId);
        if (game.getStatus() != GameStatus.FINISHED) {
            finishGameInternal(game);
            broadcast(game);
        }
        return toResults(game);
    }

    @Override
    public GameResults getResults(Long gameId) {
        return toResults(getById(gameId));
    }

    @Override
    public List<GameSummary> lobbyGames() {
        return gameRepo.findAll().stream()
                .filter(game -> game.getStatus() != GameStatus.FINISHED)
                .map(GameSummary::from)
                .toList();
    }

    @Override
    public Game getById(Long id) {
        return gameRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Game not found: " + id));
    }

    @Override
    public void requireParticipant(Long gameId, Long playerId) {
        Game game = getById(gameId);
        if (playerId == null || game.getPlayers().stream().noneMatch(p -> p.getId().equals(playerId))) {
            throw new AccessDeniedException("You are not a player in this game");
        }
    }

    @Override
    public void autoAdvanceExpiredGames() {
        long now = System.currentTimeMillis();
        List<Long> expiredGameIds = gameRepo.findAll().stream()
                .filter(game -> game.getStatus() == GameStatus.IN_PROGRESS)
                .filter(game -> roundExpired(game, now))
                .map(Game::getId)
                .toList();

        for (Long gameId : expiredGameIds) {
            // Re-lock and re-check: another request may have advanced it already.
            Game game = gameRepo.findByIdForUpdate(gameId).orElse(null);
            if (game == null || game.getStatus() != GameStatus.IN_PROGRESS || !roundExpired(game, now)) {
                continue;
            }
            advanceRound(game, null);
        }
    }

    @Override
    public void expireStaleWaitingRooms() {
        long now = System.currentTimeMillis();
        List<Game> stale = gameRepo.findAll().stream()
                .filter(game -> game.getStatus() == GameStatus.WAITING)
                .filter(game -> now - game.getCreatedAt() >= WAITING_ROOM_TTL_MS)
                .toList();
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(game -> gameRepo.delete(game));
        publisher.publishLobby(lobbyGames());
    }

    /* ------------------------------------------------------------------ */
    /* Internals                                                          */
    /* ------------------------------------------------------------------ */

    /** Advance to the next round or finish the game, then broadcast the new state. */
    private RoundResponse advanceRound(Game game, Long playerId) {
        if (game.getCurrentRound() >= game.getTotalRounds() || startRound(game) == null) {
            finishGameInternal(game);
        } else {
            gameRepo.save(game);
        }

        RoundResponse response = buildRound(game, playerId);
        broadcast(game);
        return response;
    }

    /** Build a fresh game from the request filters and add the host player. */
    private Game newGame(GameStartRequest request, Player player) {
        Category category = parseEnum(Category.class, request.getCategory());
        Language language = parseEnum(Language.class, request.getLanguage());
        Integer difficulty = request.getDifficulty();

        List<Question> pool = questionPool(category, difficulty, language);
        if (pool.isEmpty()) {
            throw new IllegalArgumentException(
                    "No questions available for the selected category/difficulty");
        }

        Game game = new Game();
        game.setCreatedAt(System.currentTimeMillis());
        game.setCategory(category);
        game.setDifficulty(difficulty);
        game.setLanguage(language);
        int requestedRounds = request.getRounds() != null ? request.getRounds() : DEFAULT_ROUNDS;
        game.setTotalRounds(Math.max(1, Math.min(requestedRounds, pool.size())));
        game.setHostPlayerId(player.getId());
        game.getPlayers().add(player);
        return game;
    }

    /** Push the game state and the lobby list to subscribers. */
    private void broadcast(Game game) {
        publisher.publishGame(game.getId(), buildRound(game, null));
        publisher.publishLobby(lobbyGames());
    }

    /**
     * Pick an unused question matching the game filters and move to the next
     * round. Returns the selected question, or {@code null} when the pool is
     * exhausted (meaning the game should end).
     */
    private Question startRound(Game game) {
        Set<Long> used = new HashSet<>(game.getUsedQuestionIds());
        List<Question> candidates = questionPool(game.getCategory(), game.getDifficulty(), game.getLanguage())
                .stream()
                .filter(q -> !used.contains(q.getId()))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        Question selected = candidates.get(random.nextInt(candidates.size()));
        game.setCurrentRound(game.getCurrentRound() + 1);
        game.setCurrentQuestionId(selected.getId());
        game.getUsedQuestionIds().add(selected.getId());
        game.getAnsweredPlayerIds().clear();
        game.setRoundStartedAt(System.currentTimeMillis());
        return selected;
    }

    private void finishGameInternal(Game game) {
        game.setStatus(GameStatus.FINISHED);

        // Rank every participant (not just those who scored) so a player with a
        // zero score still gets a history row and appears on the leaderboard.
        List<Player> ranking = new ArrayList<>(game.getPlayers());
        ranking.sort(Comparator
                .comparingInt((Player p) -> game.getScores().getOrDefault(p.getId(), 0))
                .reversed());

        Long winnerId = ranking.isEmpty() ? null : ranking.get(0).getId();
        game.setWinnerId(winnerId);
        gameRepo.save(game);

        int rank = 1;
        for (Player player : ranking) {
            GameHistory history = new GameHistory();
            history.setGameId(game.getId());
            history.setPlayerId(player.getId());
            history.setFinalScore(game.getScores().getOrDefault(player.getId(), 0));
            history.setRank(rank++);
            history.setCorrectAnswers(game.getCorrectAnswers().getOrDefault(player.getId(), 0));
            history.setTotalRounds(game.getTotalRounds());
            history.setPlayedAt(LocalDateTime.now());
            gameHistoryRepo.save(history);
        }
    }

    /** Remove a player and all of their per-round state from the game. */
    private void removePlayer(Game game, Long playerId) {
        game.getPlayers().removeIf(p -> p.getId().equals(playerId));
        game.getScores().remove(playerId);
        game.getCorrectAnswers().remove(playerId);
        game.getAnsweredPlayerIds().remove(playerId);
    }

    private RoundResponse buildRound(Game game, Long playerId) {
        RoundResponse response = new RoundResponse();
        response.setGameId(game.getId());
        response.setStatus(game.getStatus().name());
        response.setRound(game.getCurrentRound());
        response.setTotalRounds(game.getTotalRounds());
        response.setPlayerId(playerId);
        response.setHostPlayerId(game.getHostPlayerId());
        response.setPlayers(roster(game));
        response.setAnsweredPlayerIds(List.copyOf(game.getAnsweredPlayerIds()));
        response.setFinished(game.getStatus() == GameStatus.FINISHED);
        response.setRoundStartedAt(game.getRoundStartedAt());
        response.setRoundTimeoutSeconds(game.getRoundTimeoutSeconds());

        if (!response.isFinished() && game.getCurrentQuestionId() != null) {
            questionRepo.findById(game.getCurrentQuestionId())
                    .ifPresent(q -> response.setQuestion(QuestionResponse.from(q)));
        }
        return response;
    }

    private GameResults toResults(Game game) {
        GameResults results = new GameResults();
        results.setGameId(game.getId());
        results.setStatus(game.getStatus().name());
        results.setTotalRounds(game.getTotalRounds());
        results.setScores(roster(game));
        results.setWinnerId(game.getWinnerId());
        if (game.getWinnerId() != null) {
            playerRepo.findById(game.getWinnerId()).ifPresent(p -> results.setWinnerName(p.getName()));
        }
        return results;
    }

    private List<PlayerScore> roster(Game game) {
        return game.getPlayers().stream()
                .map(player -> new PlayerScore(
                        player.getId(),
                        player.getName(),
                        game.getScores().getOrDefault(player.getId(), 0),
                        game.getCorrectAnswers().getOrDefault(player.getId(), 0)))
                .sorted(Comparator.comparingInt(PlayerScore::getScore).reversed())
                .toList();
    }

    private List<Question> questionPool(Category category, Integer difficulty, Language language) {
        return questionRepo.findAll().stream()
                .filter(q -> category == null || q.getCategory() == category)
                .filter(q -> difficulty == null || q.getDifficulty() == difficulty)
                .filter(q -> language == null || q.getLanguage() == language)
                .toList();
    }

    private Player resolvePlayer(Long playerId, String playerName) {
        if (playerId != null) {
            Optional<Player> existing = playerRepo.findById(playerId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Player player = new Player();
        player.setName(playerName != null && !playerName.isBlank() ? playerName.trim() : "Guest");
        return playerRepo.save(player);
    }

    private void requirePlayer(Game game, Long playerId) {
        if (playerId == null || game.getPlayers().stream().noneMatch(p -> p.getId().equals(playerId))) {
            throw new IllegalArgumentException("Player is not part of this game");
        }
    }

    private List<Long> playerIds(Game game) {
        return game.getPlayers().stream().map(Player::getId).toList();
    }

    /** Whether the current round has run past its timeout. */
    private boolean roundExpired(Game game, long now) {
        return game.getRoundStartedAt() > 0
                && now - game.getRoundStartedAt() >= game.getRoundTimeoutSeconds() * 1000L;
    }

    /** Server-measured elapsed time for the current round, capped at the timeout. */
    private long serverElapsedMs(Game game) {
        if (game.getRoundStartedAt() <= 0) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - game.getRoundStartedAt();
        long cap = game.getRoundTimeoutSeconds() * 1000L;
        return cap > 0 ? Math.min(elapsed, cap) : elapsed;
    }

    /** Load a game and lock it for the duration of the transaction. */
    private Game getForUpdate(Long id) {
        return gameRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Game not found: " + id));
    }

    private boolean isCorrect(String expected, String given) {
        if (expected == null || given == null) {
            return false;
        }
        return expected.trim().equalsIgnoreCase(given.trim());
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null; // unknown value -> "any"
        }
    }
}
