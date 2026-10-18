package com.march.backend.service;

import com.march.backend.model.Question;
import java.util.List;

public interface QuestionService {
    List<Question> getAllQuestions();
    Question getQuestionById(Long id);
    Question createQuestion(Question question);
    void deleteQuestion(Long id);
    /**
     * Retrieve the hint for a specific question.
     */
    String getHint(Long questionId);
}