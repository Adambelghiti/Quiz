package com.march.backend.repository;

import com.march.backend.model.Language;
import com.march.backend.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Natural key of a question: its text within one language. Used by the
     * seeder to extend an existing question bank without creating duplicates and
     * without overwriting questions an administrator has edited.
     */
    Optional<Question> findByTextAndLanguage(String text, Language language);
}
