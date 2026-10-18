package com.march.backend.dto;

import com.march.backend.model.Question;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Question as sent to the client during a game. Deliberately omits
 * {@code correctAnswer} so the answer cannot be read from the network tab.
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionResponse {

    private Long id;
    private String text;
    private String hint;
    private List<String> options;
    private String category;
    private int difficulty;
    private String language;

    public static QuestionResponse from(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.id = question.getId();
        response.text = question.getText();
        response.hint = question.getHint();
        response.options = question.getOptions();
        response.category = question.getCategory() != null ? question.getCategory().name() : null;
        response.difficulty = question.getDifficulty() != null ? question.getDifficulty() : 0;
        response.language = question.getLanguage() != null ? question.getLanguage().name() : null;
        return response;
    }
}
