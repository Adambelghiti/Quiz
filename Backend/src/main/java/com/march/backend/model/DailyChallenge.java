package com.march.backend.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The shared question set for one UTC day. It is created once and then reused by
 * every player, so results on the daily leaderboard are comparable.
 */
@Entity
@Table(name = "daily_challenges")
@Getter
@Setter
@NoArgsConstructor
public class DailyChallenge {

    @Id
    @Column(name = "challenge_date")
    private LocalDate challengeDate;

    @Enumerated(EnumType.STRING)
    private Language language = Language.EN;

    private int totalRounds;

    private int roundTimeoutSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "daily_challenge_questions",
            joinColumns = @JoinColumn(name = "challenge_date"))
    @OrderColumn(name = "position")
    @Column(name = "question_id")
    private List<Long> questionIds = new ArrayList<>();
}
