package com.march.backend.controller;

import com.march.backend.model.Category;
import com.march.backend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exposes endpoints for retrieving available categories and difficulty levels.
 */
@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private QuestionService questionService;

    /**
     * Return a list of category names (strings). The backend stores categories as an enum.
     */
    @GetMapping("/categories")
    public List<String> getCategories() {
        return questionService.getAllQuestions().stream()
                .map(q -> q.getCategory().name())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Return a list of supported difficulty levels. For now hard‑coded to 1-3.
     */
    @GetMapping("/difficulties")
    public List<Integer> getDifficulties() {
        return List.of(1, 2, 3);
    }
}