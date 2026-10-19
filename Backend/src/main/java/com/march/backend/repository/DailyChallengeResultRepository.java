package com.march.backend.repository;

import com.march.backend.model.DailyChallengeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyChallengeResultRepository extends JpaRepository<DailyChallengeResult, Long> {

    List<DailyChallengeResult> findByChallengeDateOrderByScoreDescCompletedAtAsc(LocalDate challengeDate);

    Optional<DailyChallengeResult> findByChallengeDateAndPlayerId(LocalDate challengeDate, Long playerId);
}
