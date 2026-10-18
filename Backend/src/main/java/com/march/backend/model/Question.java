package com.march.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * A single quiz question.
 * <p>
 * The constraints below are enforced both when an administrator posts a
 * question (via {@code @Valid}) and by Hibernate when the entity is flushed, so
 * no code path can persist a question the game could not present or grade.
 * The one rule that needs both fields at once - the correct answer must be one
 * of the options - lives in {@code QuestionServiceImpl}.
 */
@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Question text is required")
    private String text;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    private Category category;

    @Min(value = 1, message = "Difficulty must be between 1 and 3")
    @Max(value = 3, message = "Difficulty must be between 1 and 3")
    private int difficulty; // 1-3

    @NotNull(message = "Language is required")
    @Enumerated(EnumType.STRING)
    private Language language;

    @NotBlank(message = "A correct answer is required")
    private String correctAnswer;

    @NotEmpty(message = "At least two answer options are required")
    @Size(min = 2, message = "At least two answer options are required")
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options;

    private String hint; // optional
}
