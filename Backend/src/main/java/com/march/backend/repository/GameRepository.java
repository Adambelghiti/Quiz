package com.march.backend.repository;

import com.march.backend.model.Game;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Load a game and take a write lock on it for the duration of the current
     * transaction. Serialises concurrent mutations (e.g. simultaneous answers)
     * so no score update is lost.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.id = :id")
    Optional<Game> findByIdForUpdate(@Param("id") Long id);

    /**
     * Number of times the given player appears in the given game.
     * <p>
     * Used for authorization checks (e.g. STOMP subscriptions) where the caller
     * is not inside a transaction: loading the game and touching its lazy
     * {@code players} collection would fail with a {@code LazyInitializationException}.
     * A count query needs no session and no lazy initialisation.
     */
    @Query("select count(p) from Game g join g.players p where g.id = :gameId and p.id = :playerId")
    long countPlayersInGame(@Param("gameId") Long gameId, @Param("playerId") Long playerId);
}
