package com.march.backend.repository;

import com.march.backend.model.DailyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, LocalDate> {
}
