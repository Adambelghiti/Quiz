package com.march.backend.controller;

import com.march.backend.dto.QuestionResponse;
import com.march.backend.dto.QuizFacets;
import com.march.backend.model.Category;
import com.march.backend.model.Language;
import com.march.backend.model.Question;
import com.march.backend.service.QuestionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller exposing CRUD operations for {@link Question}.
 * <p>
 * Read endpoints return {@link QuestionResponse} so the correct answer is never
 * sent to the client. Mutating endpoints are restricted to administrators (see
 * {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /** Retrieve all questions, optionally filtered by category, difficulty and/or language. */
    @GetMapping
    public List<QuestionResponse> getAll(
            @RequestParam Optional<Category> category,
            @RequestParam Optional<Integer> difficulty,
            @RequestParam Optional<Language> language) {
        return questionService.getAllQuestions().stream()
                .filter(q -> category.isEmpty() || q.getCategory() == category.get())
                .filter(q -> difficulty.isEmpty() || q.getDifficulty() == difficulty.get())
                .filter(q -> language.isEmpty() || q.getLanguage() == language.get())
                .map(QuestionResponse::from)
                .toList();
    }

    /**
     * The filter values that actually have questions. Used by the setup screen so
     * it never offers a combination that would return an empty pool.
     * <p>
     * Legacy rows without a category or language are skipped rather than thrown
     * on, so one incomplete question cannot break the whole setup screen.
     */
    @GetMapping("/facets")
    public QuizFacets getFacets() {
        List<Question> all = questionService.getAllQuestions();
        List<String> categories = all.stream()
                .map(Question::getCategory)
                .filter(Objects::nonNull)
                .map(Category::name)
                .distinct()
                .sorted()
                .toList();
        List<Integer> difficulties = all.stream()
                .map(Question::getDifficulty)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        List<String> languages = all.stream()
                .map(Question::getLanguage)
                .filter(Objects::nonNull)
                .map(Language::name)
                .distinct()
                .sorted()
                .toList();
        return new QuizFacets(categories, difficulties, languages);
    }

    /** Full questions including answers; administrators only. */
    @GetMapping("/admin")
    public List<Question> getAllForAdmin() {
        return questionService.getAllQuestions();
    }

    /** Retrieve a single question by id (without the answer). */
    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getById(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        return question != null
                ? ResponseEntity.ok(QuestionResponse.from(question))
                : ResponseEntity.notFound().build();
    }

    /** Create a new question; administrators only. */
    @PostMapping
    public ResponseEntity<Question> create(@Valid @RequestBody Question question) {
        return ResponseEntity.ok(questionService.createQuestion(question));
    }

    /** Update an existing question; administrators only. */
    @PutMapping("/{id}")
    public ResponseEntity<Question> update(@PathVariable Long id, @Valid @RequestBody Question question) {
        Question existing = questionService.getQuestionById(id);
        if (existing == null) {
            throw new EntityNotFoundException("Question not found: " + id);
        }
        existing.setText(question.getText());
        existing.setCategory(question.getCategory());
        existing.setDifficulty(question.getDifficulty());
        existing.setLanguage(question.getLanguage());
        existing.setCorrectAnswer(question.getCorrectAnswer());
        existing.setOptions(question.getOptions());
        existing.setHint(question.getHint());
        return ResponseEntity.ok(questionService.createQuestion(existing));
    }

    /** Delete a question by id; administrators only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    /** Return the hint for a specific question. */
    @GetMapping("/{id}/hint")
    public ResponseEntity<String> getHint(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getHint(id));
    }
}
