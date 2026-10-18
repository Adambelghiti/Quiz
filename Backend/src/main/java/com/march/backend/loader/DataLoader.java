package com.march.backend.loader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.march.backend.model.*;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.repository.QuestionRepository;

/**
 * Seeds development data on startup: a question bank, one demo account and one
 * admin account so the app can be used immediately.
 */
@Component
public class DataLoader implements CommandLineRunner {

    /** Convenience player account created on first run. */
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo123";

    /** Convenience administrator account created on first run. */
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin123";

    private final QuestionRepository repo;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(QuestionRepository repo,
                      AuthUserRepository authUserRepository,
                      PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedQuestions();
        seedAccount(DEMO_USERNAME, DEMO_PASSWORD, "USER");
        seedAccount(ADMIN_USERNAME, ADMIN_PASSWORD, "ADMIN");
    }

    private void seedQuestions() {
        if (repo.count() > 0) {
            return;
        }

        // --- Easy (difficulty 1) -------------------------------------------------
        repo.save(q("What is the capital of Morocco?", "Use a map.", "Rabat",
                Category.MOROCCAN_HISTORY, 1, "Casablanca", "Fez", "Marrakech", "Rabat"));
        repo.save(q("Which desert covers most of northern Africa?", "It's the largest in Africa.",
                "Sahara", Category.GEOGRAPHY, 1, "Kalahari", "Namib", "Sahara", "Atacama"));
        repo.save(q("Which is the largest city in Morocco?", "It is not the capital.",
                "Casablanca", Category.MOROCCAN_CULTURE, 1, "Rabat", "Casablanca", "Fez", "Tangier"));
        repo.save(q("Which body of water borders Morocco to the west?", "Think of a large ocean.",
                "Atlantic Ocean", Category.GEOGRAPHY, 1, "Red Sea", "Atlantic Ocean", "Mediterranean Sea", "Indian Ocean"));
        repo.save(q("How many continents are there on Earth?", "Count them all.",
                "7", Category.GENERAL_KNOWLEDGE, 1, "5", "6", "7", "8"));
        repo.save(q("Who wrote the play 'Romeo and Juliet'?", "An English playwright.",
                "William Shakespeare", Category.GENERAL_KNOWLEDGE, 1,
                "Charles Dickens", "William Shakespeare", "Jane Austen", "Oscar Wilde"));
        repo.save(q("Which planet is known as the Red Planet?", "It is the fourth planet from the Sun.",
                "Mars", Category.SCIENCE, 1, "Venus", "Mars", "Jupiter", "Mercury"));
        repo.save(q("Which country hosted the 2022 FIFA World Cup?", "It is in the Middle East.",
                "Qatar", Category.SPORTS, 1, "Qatar", "UAE", "Saudi Arabia", "Russia"));
        repo.save(q("How many strings does a standard guitar have?", "A classic rock instrument.",
                "6", Category.MUSIC, 1, "4", "5", "6", "7"));

        // --- Medium (difficulty 2) ----------------------------------------------
        repo.save(q("What is the national language of Morocco?", "It is an official language.",
                "Arabic", Category.MOROCCAN_CULTURE, 2, "Arabic", "French", "Spanish", "English"));
        repo.save(q("In which year did Morocco gain independence?", "Mid-20th century.",
                "1956", Category.MOROCCAN_HISTORY, 2, "1912", "1956", "1962", "1975"));
        repo.save(q("What is the highest mountain in Morocco?", "It is in the Atlas mountains.",
                "Jbel Toubkal", Category.GEOGRAPHY, 2, "Jbel Toubkal", "Jbel Ayachi", "Mount Ararat", "Rif Peak"));
        repo.save(q("What is the chemical symbol for gold?", "From the Latin 'aurum'.",
                "Au", Category.SCIENCE, 2, "Ag", "Au", "Gd", "Go"));
        repo.save(q("What is the largest organ of the human body?", "It covers you.",
                "Skin", Category.SCIENCE, 2, "Liver", "Heart", "Skin", "Lungs"));
        repo.save(q("Which Moroccan city is famous for its tanneries?", "An imperial city.",
                "Fez", Category.MOROCCAN_CULTURE, 2, "Fez", "Agadir", "Rabat", "Tangier"));

        // --- Hard (difficulty 3) -------------------------------------------------
        repo.save(q("In which year did the Berlin Wall fall?", "The end of the Cold War era.",
                "1989", Category.GENERAL_KNOWLEDGE, 3, "1985", "1989", "1991", "1993"));
        repo.save(q("Which is the longest river in Africa?", "It flows north into the Mediterranean.",
                "Nile", Category.GEOGRAPHY, 3, "Congo", "Niger", "Nile", "Zambezi"));
        repo.save(q("What is the approximate speed of light in a vacuum (km/s)?", "Roughly 3 x 10^5.",
                "300,000", Category.SCIENCE, 3, "30,000", "300,000", "3,000,000", "1,080,000,000"));
        repo.save(q("Which Moroccan dynasty built the Koutoubia Mosque in Marrakech?",
                "They also built the Giralda in Seville.", "Almohad", Category.MOROCCAN_HISTORY, 3,
                "Idrisid", "Almoravid", "Almohad", "Saadi"));
        repo.save(q("Who composed the 'Brandenburg Concertos'?", "A German Baroque composer.",
                "Johann Sebastian Bach", Category.MUSIC, 3,
                "Wolfgang Amadeus Mozart", "Johann Sebastian Bach", "Ludwig van Beethoven", "Georg Handel"));
        repo.save(q("How many players are on the field per team in a rugby union match?",
                "It is more than a football team.", "15", Category.SPORTS, 3, "11", "13", "15", "18"));

        // --- French translations (a small set, to exercise the language filter) --
        repo.save(q("Quelle est la capitale du Maroc ?", "Utilisez une carte.", "Rabat",
                Category.MOROCCAN_HISTORY, 1, Language.FR, "Casablanca", "Fez", "Marrakech", "Rabat"));
        repo.save(q("Quel désert couvre la majeure partie du nord de l'Afrique ?", "Le plus grand d'Afrique.",
                "Sahara", Category.GEOGRAPHY, 1, Language.FR, "Kalahari", "Namib", "Sahara", "Atacama"));
        repo.save(q("Quel est le symbole chimique de l'or ?", "Du latin 'aurum'.",
                "Au", Category.SCIENCE, 2, Language.FR, "Ag", "Au", "Gd", "Go"));

        // --- Arabic translations (a small set, to exercise the language filter) ---
        repo.save(q("ما هي عاصمة المغرب؟", "استخدم الخريطة.", "الرباط",
                Category.MOROCCAN_HISTORY, 1, Language.AR, "الدار البيضاء", "فاس", "مراكش", "الرباط"));
        repo.save(q("ما هو أكبر محيط يحد المغرب غربا؟", "فكر في محيط كبير.",
                "المحيط الأطلسي", Category.GEOGRAPHY, 1, Language.AR,
                "البحر الأحمر", "المحيط الأطلسي", "البحر الأبيض المتوسط", "المحيط الهندي"));
        repo.save(q("من كتب مسرحية روميو وجولييت؟", "كاتب مسرحي إنجليزي.",
                "وليام شكسبير", Category.GENERAL_KNOWLEDGE, 1, Language.AR,
                "تشارلز ديكنز", "وليام شكسبير", "جين أوستن", "أوسكار وايلد"));
    }

    private void seedAccount(String username, String password, String role) {
        if (authUserRepository.existsByUsername(username)) {
            return;
        }
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        authUserRepository.save(user);
    }

    private Question q(String text, String hint, String correctAnswer,
                       Category category, int difficulty, String... options) {
        return q(text, hint, correctAnswer, category, difficulty, Language.EN, options);
    }

    private Question q(String text, String hint, String correctAnswer,
                       Category category, int difficulty, Language language, String... options) {
        Question question = new Question();
        question.setText(text);
        question.setHint(hint);
        question.setCorrectAnswer(correctAnswer);
        question.setCategory(category);
        question.setDifficulty(difficulty);
        question.setLanguage(language);
        question.setOptions(java.util.List.of(options));
        return question;
    }
}
