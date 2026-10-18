package com.march.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a quiz game: a set of players answering a sequence of questions
 * chosen from an optional category / difficulty / language filter.
 */
@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optimistic-lock guard; bumped on every state change. */
    @Version
    private long version;

    /** Epoch millis when the game/room was created (used to expire stale rooms). */
    private long createdAt = System.currentTimeMillis();

    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.WAITING;

    /** Filters chosen when the game was created. {@code null} means "any". */
    @Enumerated(EnumType.STRING)
    private Category category;

    private Integer difficulty;

    @Enumerated(EnumType.STRING)
    private Language language;

    /** How many rounds (questions) this game lasts. */
    private int totalRounds = 5;

    /** Players that joined the game. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id"))
    private List<Player> players = new ArrayList<>();

    /** Current round number (1-based, 0 before the first round starts). */
    private int currentRound = 0;

    /** Id of the question currently being answered. */
    private Long currentQuestionId;

    /** Epoch millis when the current round began; 0 when no round is active. */
    private long roundStartedAt = 0L;

    /** Seconds allowed per round before it auto-advances. */
    private int roundTimeoutSeconds = 30;

    /** Ids of questions already used in this game. */
    @ElementCollection
    @CollectionTable(name = "game_used_questions", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "question_id")
    private List<Long> usedQuestionIds = new ArrayList<>();

    /** Players that have already submitted an answer for the current round. */
    @ElementCollection
    @CollectionTable(name = "game_answered_players", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "player_id")
    private List<Long> answeredPlayerIds = new ArrayList<>();

    /** Points earned per player during the game. */
    @ElementCollection
    @CollectionTable(name = "game_scores", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyColumn(name = "player_id")
    @Column(name = "score")
    private Map<Long, Integer> scores = new HashMap<>();

    /** Number of correct answers per player. */
    @ElementCollection
    @CollectionTable(name = "game_correct_answers", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyColumn(name = "player_id")
    @Column(name = "correct_count")
    private Map<Long, Integer> correctAnswers = new HashMap<>();

    /** Optional winner id; set when the game finishes. */
    private Long winnerId;

    /** Player that created the room; only the host may start the game. */
    private Long hostPlayerId;
}
