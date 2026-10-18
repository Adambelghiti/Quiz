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
        return questionRepository.save(question);
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