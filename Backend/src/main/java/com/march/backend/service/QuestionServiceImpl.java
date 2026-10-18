package com.march.backend.service;

import com.march.backend.model.Question;
import com.march.backend.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionServiceImpl implements QuestionService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Override
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }
    
    @Override
    public Question getQuestionById(Long id) {
        Optional<Question> question = questionRepository.findById(id);
        return question.orElse(null);
    }
    
    @Override
    public Question createQuestion(Question question) {
        requireAnswerAmongOptions(question);
        return questionRepository.save(question);
    }

    /**
     * Cross-field rule: the correct answer has to match one of the offered
     * options, otherwise the question could never be answered correctly.
     * Mirrors the check the admin screen already performs client-side.
     */
    private void requireAnswerAmongOptions(Question question) {
        List<String> options = question.getOptions();
        String answer = question.getCorrectAnswer();
        boolean offered = options != null && answer != null && options.stream()
                .anyMatch(option -> option != null && option.trim().equals(answer.trim()));
        if (!offered) {
            throw new IllegalArgumentException("The correct answer must match one of the options");
        }
    }
    
    @Override
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    @Override
    public String getHint(Long questionId) {
        Optional<Question> opt = questionRepository.findById(questionId);
        return opt.map(Question::getHint).orElse("No hint available");
    }
}