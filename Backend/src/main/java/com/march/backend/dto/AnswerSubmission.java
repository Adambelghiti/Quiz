package com.march.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of a {@code POST /api/games/{id}/answer} request.
 * <p>
 * The acting player is taken from the authenticated principal, so no player id
 * is accepted here. Elapsed time is measured on the server and the client value
 * is ignored.
 */
@Getter
@Setter
@NoArgsConstructor
public class AnswerSubmission {

    @NotNull(message = "questionId is required")
    private Long questionId;

    @NotBlank(message = "answer is required")
    private String answer;

    /** Retained for compatibility; ignored in favour of the server clock. */
    private long elapsedMs;
}
