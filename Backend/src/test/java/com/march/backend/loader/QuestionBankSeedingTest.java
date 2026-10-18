package com.march.backend.loader;

import com.march.backend.model.Category;
import com.march.backend.model.Language;
import com.march.backend.model.Question;
import com.march.backend.repository.QuestionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how the built-in question bank is applied to a database, and that the
 * bank itself is playable.
 * <p>
 * The tests run in a transaction that is rolled back, so reducing the bank here
 * cannot leak into other test classes.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionBankSeedingTest {

    @Autowired
    private DataLoader dataLoader;
    @Autowired
    private QuestionRepository repo;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void seedingIsIdempotent() {
        long before = repo.count();

        dataLoader.run();

        assertEquals(before, repo.count(), "re-seeding an up-to-date database must not add rows");

        List<String> keys = repo.findAll().stream()
                .map(q -> q.getText() + "|" + q.getLanguage())
                .toList();
        assertEquals(keys.size(), new HashSet<>(keys).size(),
                "the bank must not contain duplicate (text, language) entries");
    }

    @Test
    void seedingExtendsAnAlreadyPopulatedDatabase() {
        List<Question> all = repo.findAll();
        Question kept = all.get(0);
        repo.deleteAll(all.stream().filter(q -> !q.getId().equals(kept.getId())).toList());
        assertEquals(1, repo.count(), "precondition: only one question is left");

        dataLoader.run();

        assertTrue(repo.count() > 1,
                "a database that already has questions must be extended, not skipped");
        assertTrue(repo.findById(kept.getId()).isPresent(),
                "the pre-existing question must still be there");
    }

    @Test
    void existingQuestionsAreNeverOverwritten() {
        Question question = repo.findAll().stream()
                .filter(q -> q.getCategory() == Category.ISLAMIC_CULTURE)
                .findFirst()
                .orElseThrow();
        Long id = question.getId();
        String editedHint = "edited-by-an-administrator";

        question.setHint(editedHint);
        repo.saveAndFlush(question);

        dataLoader.run();
        entityManager.clear();

        assertEquals(editedHint, repo.findById(id).orElseThrow().getHint(),
                "seeding must never overwrite a question an administrator has edited");
    }

    @Test
    void everyCategoryHasQuestions() {
        Set<Category> covered = repo.findAll().stream()
                .map(Question::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Category category : Category.values()) {
            assertTrue(covered.contains(category),
                    "category " + category + " should have at least one question");
        }
    }

    @Test
    void theTwoNewCategoriesAreAvailableInEveryLanguage() {
        List<Question> all = repo.findAll();

        for (Category category : List.of(Category.ARABIC_CULTURE, Category.ISLAMIC_CULTURE)) {
            Set<Language> languages = all.stream()
                    .filter(q -> q.getCategory() == category)
                    .map(Question::getLanguage)
                    .collect(Collectors.toSet());

            for (Language language : Language.values()) {
                assertTrue(languages.contains(language),
                        category + " should have a " + language + " question");
            }
        }
    }

    @Test
    void everySeededQuestionIsPlayable() {
        for (Question question : repo.findAll()) {
            String where = "question " + question.getId();

            assertNotNull(question.getText(), where + " needs text");
            assertFalse(question.getText().isBlank(), where + " needs text");
            assertNotNull(question.getCategory(), where + " needs a category");
            assertNotNull(question.getLanguage(), where + " needs a language");
            assertTrue(question.getDifficulty() >= 1 && question.getDifficulty() <= 3,
                    where + " needs a difficulty between 1 and 3");
            assertNotNull(question.getCorrectAnswer(), where + " needs a correct answer");
            assertFalse(question.getCorrectAnswer().isBlank(), where + " needs a correct answer");
            assertNotNull(question.getOptions(), where + " needs options");
            assertTrue(question.getOptions().size() >= 2, where + " needs at least two options");
            assertTrue(
                    question.getOptions().stream().anyMatch(option ->
                            option != null && option.trim().equals(question.getCorrectAnswer().trim())),
                    where + " must offer its correct answer as one of the options");
        }
    }
}
