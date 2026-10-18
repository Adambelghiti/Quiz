package com.march.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.march.backend.model.GameHistory;
import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    List<GameHistory> findByPlayerId(Long playerId);

    List<GameHistory> findByPlayerIdOrderByPlayedAtDesc(Long playerId);
}