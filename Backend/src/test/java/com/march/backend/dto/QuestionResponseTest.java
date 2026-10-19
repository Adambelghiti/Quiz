package com.march.backend.dto;

import com.march.backend.model.Category;
import com.march.backend.model.Language;
import com.march.backend.model.Question;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The element collection has no persisted position, so the database may return
 * an answer's options in any order. The API must still present them in a stable
 * order so the screen and the read-aloud voice always agree.
 */
class QuestionResponseTest {

    @Test
    void optionsAreReturnedInAStableAlphabeticalOrder() {
        Question question = new Question();
        question.setText("Pick a fruit?");
        question.setCategory(Category.GENERAL_KNOWLEDGE);
        question.setDifficulty(1);
        question.setLanguage(Language.EN);
        question.setCorrectAnswer("Apple");
        question.setOptions(List.of("Zebra fruit", "Apple", "Mango"));

        QuestionResponse response = QuestionResponse.from(question);

        assertEquals(List.of("Apple", "Mango", "Zebra fruit"), response.getOptions());
    }

    @Test
    void theExplanationIsNotSentBeforeThePlayerAnswers() {
        Question question = new Question();
        question.setText("Pick a fruit?");
        question.setExplanation("This text would give the answer away.");
        question.setOptions(List.of("Apple", "Banana"));

        String json = new ObjectMapper().writeValueAsString(QuestionResponse.from(question));

        assertFalse(json.contains("explanation"), "the explanation must only arrive with the answer result");
        assertFalse(json.contains("give the answer away"), "the explanation text must not leak with the question");
    }

    @Test
    void aQuestionWithoutOptionsDoesNotFail() {
        Question question = new Question();
        question.setText("Empty?");
        question.setOptions(null);

        QuestionResponse response = QuestionResponse.from(question);

        assertEquals(List.of(), response.getOptions());
    }
}
