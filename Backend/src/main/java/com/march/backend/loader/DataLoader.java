package com.march.backend.loader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.march.backend.model.*;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds development data on startup: a question bank, one demo account and one
 * admin account so the app can be used immediately.
 * <p>
 * Seeding is idempotent and <em>additive</em>. Questions are matched on
 * {@code (text, language)}: one that is already present is left exactly as it is
 * (so an administrator's edits are never overwritten) and only genuinely new
 * questions are inserted. An existing database is therefore extended with new
 * bank content on the next start instead of being skipped, and restarting the
 * application never creates duplicates.
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

    /**
     * Insert every bank question that is not in the database yet. Existing
     * questions are skipped, never modified.
     */
    private void seedQuestions() {
        for (Question question : questionBank()) {
            if (repo.findByTextAndLanguage(question.getText(), question.getLanguage()).isEmpty()) {
                repo.save(question);
            }
        }
    }

    /** The built-in question bank; {@code (text, language)} makes each entry unique. */
    private List<Question> questionBank() {
        List<Question> bank = new ArrayList<>();

        // --- English: easy (difficulty 1) ---------------------------------------
        bank.add(q("What is the capital of Morocco?", "Use a map.", "Rabat",
                Category.MOROCCAN_HISTORY, 1, "Casablanca", "Fez", "Marrakech", "Rabat"));
        bank.add(q("Which desert covers most of northern Africa?", "It's the largest in Africa.",
                "Sahara", Category.GEOGRAPHY, 1, "Kalahari", "Namib", "Sahara", "Atacama"));
        bank.add(q("Which is the largest city in Morocco?", "It is not the capital.",
                "Casablanca", Category.MOROCCAN_CULTURE, 1, "Rabat", "Casablanca", "Fez", "Tangier"));
        bank.add(q("Which body of water borders Morocco to the west?", "Think of a large ocean.",
                "Atlantic Ocean", Category.GEOGRAPHY, 1, "Red Sea", "Atlantic Ocean", "Mediterranean Sea", "Indian Ocean"));
        bank.add(q("How many continents are there on Earth?", "Count them all.",
                "7", Category.GENERAL_KNOWLEDGE, 1, "5", "6", "7", "8"));
        bank.add(q("Who wrote the play 'Romeo and Juliet'?", "An English playwright.",
                "William Shakespeare", Category.GENERAL_KNOWLEDGE, 1,
                "Charles Dickens", "William Shakespeare", "Jane Austen", "Oscar Wilde"));
        bank.add(q("Which planet is known as the Red Planet?", "It is the fourth planet from the Sun.",
                "Mars", Category.SCIENCE, 1, "Venus", "Mars", "Jupiter", "Mercury"));
        bank.add(q("Which country hosted the 2022 FIFA World Cup?", "It is in the Middle East.",
                "Qatar", Category.SPORTS, 1, "Qatar", "UAE", "Saudi Arabia", "Russia"));
        bank.add(q("How many strings does a standard guitar have?", "A classic rock instrument.",
                "6", Category.MUSIC, 1, "4", "5", "6", "7"));

        // --- English: medium (difficulty 2) -------------------------------------
        bank.add(q("What is the national language of Morocco?", "It is an official language.",
                "Arabic", Category.MOROCCAN_CULTURE, 2, "Arabic", "French", "Spanish", "English"));
        bank.add(q("In which year did Morocco gain independence?", "Mid-20th century.",
                "1956", Category.MOROCCAN_HISTORY, 2, "1912", "1956", "1962", "1975"));
        bank.add(q("What is the highest mountain in Morocco?", "It is in the Atlas mountains.",
                "Jbel Toubkal", Category.GEOGRAPHY, 2, "Jbel Toubkal", "Jbel Ayachi", "Mount Ararat", "Rif Peak"));
        bank.add(q("What is the chemical symbol for gold?", "From the Latin 'aurum'.",
                "Au", Category.SCIENCE, 2, "Ag", "Au", "Gd", "Go"));
        bank.add(q("What is the largest organ of the human body?", "It covers you.",
                "Skin", Category.SCIENCE, 2, "Liver", "Heart", "Skin", "Lungs"));
        bank.add(q("Which Moroccan city is famous for its tanneries?", "An imperial city.",
                "Fez", Category.MOROCCAN_CULTURE, 2, "Fez", "Agadir", "Rabat", "Tangier"));

        // --- English: hard (difficulty 3) ---------------------------------------
        bank.add(q("In which year did the Berlin Wall fall?", "The end of the Cold War era.",
                "1989", Category.GENERAL_KNOWLEDGE, 3, "1985", "1989", "1991", "1993"));
        bank.add(q("Which is the longest river in Africa?", "It flows north into the Mediterranean.",
                "Nile", Category.GEOGRAPHY, 3, "Congo", "Niger", "Nile", "Zambezi"));
        bank.add(q("What is the approximate speed of light in a vacuum (km/s)?", "Roughly 3 x 10^5.",
                "300,000", Category.SCIENCE, 3, "30,000", "300,000", "3,000,000", "1,080,000,000"));
        bank.add(q("Which Moroccan dynasty built the Koutoubia Mosque in Marrakech?",
                "They also built the Giralda in Seville.", "Almohad", Category.MOROCCAN_HISTORY, 3,
                "Idrisid", "Almoravid", "Almohad", "Saadi"));
        bank.add(q("Who composed the 'Brandenburg Concertos'?", "A German Baroque composer.",
                "Johann Sebastian Bach", Category.MUSIC, 3,
                "Wolfgang Amadeus Mozart", "Johann Sebastian Bach", "Ludwig van Beethoven", "Georg Handel"));
        bank.add(q("How many players are on the field per team in a rugby union match?",
                "It is more than a football team.", "15", Category.SPORTS, 3, "11", "13", "15", "18"));

        // --- English: Arabic and Islamic culture --------------------------------
        bank.add(q("Which language is the most widely spoken in the Arab world?",
                "It is the language of the Qur'an.", "Arabic", Category.ARABIC_CULTURE, 1,
                "Turkish", "Persian", "Arabic", "Kurdish"));
        bank.add(q("Which city is home to the headquarters of the Arab League?",
                "It is the capital of Egypt.", "Cairo", Category.ARABIC_CULTURE, 2,
                "Riyadh", "Cairo", "Amman", "Tunis"));
        bank.add(q("Which ancient Yemeni city is nicknamed the 'Manhattan of the desert'?",
                "Its mud-brick towers are a UNESCO World Heritage Site.", "Shibam",
                Category.ARABIC_CULTURE, 3, "Sana'a", "Aden", "Shibam", "Taiz"));
        bank.add(q("How many pillars does Islam have?", "They include prayer and fasting.",
                "5", Category.ISLAMIC_CULTURE, 1, "3", "4", "5", "6"));
        bank.add(q("Which city do Muslims face when they pray?", "It is in Saudi Arabia.",
                "Mecca", Category.ISLAMIC_CULTURE, 2, "Medina", "Mecca", "Jerusalem", "Cairo"));
        bank.add(q("Which scholar wrote the 'Canon of Medicine', a standard medical text in Europe for centuries?",
                "He was also known as Avicenna.", "Ibn Sina", Category.ISLAMIC_CULTURE, 3,
                "Ibn Sina", "Al-Khwarizmi", "Ibn Khaldun", "Al-Razi"));

        // --- French translations -----------------------------------------------
        bank.add(q("Quelle est la capitale du Maroc ?", "Utilisez une carte.", "Rabat",
                Category.MOROCCAN_HISTORY, 1, Language.FR, "Casablanca", "Fez", "Marrakech", "Rabat"));
        bank.add(q("Quel désert couvre la majeure partie du nord de l'Afrique ?", "Le plus grand d'Afrique.",
                "Sahara", Category.GEOGRAPHY, 1, Language.FR, "Kalahari", "Namib", "Sahara", "Atacama"));
        bank.add(q("Quel est le symbole chimique de l'or ?", "Du latin 'aurum'.",
                "Au", Category.SCIENCE, 2, Language.FR, "Ag", "Au", "Gd", "Go"));

        // --- French: Arabic and Islamic culture plus wider coverage --------------
        bank.add(q("Quelle est la langue la plus parlée dans le monde arabe ?", "C'est la langue du Coran.",
                "Arabe", Category.ARABIC_CULTURE, 1, Language.FR, "Turc", "Persan", "Arabe", "Kurde"));
        bank.add(q("Quel pays arabe est bordé par la mer Rouge et le golfe d'Aden ?", "Sa capitale est Sanaa.",
                "Yémen", Category.ARABIC_CULTURE, 2, Language.FR, "Yémen", "Oman", "Koweït", "Liban"));
        bank.add(q("Combien de piliers compte l'islam ?", "Ils incluent la prière et le jeûne.",
                "5", Category.ISLAMIC_CULTURE, 1, Language.FR, "3", "4", "5", "6"));
        bank.add(q("Vers quelle ville les musulmans se tournent-ils pour prier ?", "Elle se trouve en Arabie saoudite.",
                "La Mecque", Category.ISLAMIC_CULTURE, 2, Language.FR, "Médine", "La Mecque", "Jérusalem", "Damas"));
        bank.add(q("Quel est le plus grand océan du monde ?", "Il sépare l'Asie de l'Amérique.",
                "Pacifique", Category.GEOGRAPHY, 1, Language.FR, "Atlantique", "Pacifique", "Indien", "Arctique"));
        bank.add(q("Quelle planète est la plus proche du Soleil ?", "Elle est plus petite que la Terre.",
                "Mercure", Category.SCIENCE, 2, Language.FR, "Vénus", "Mercure", "Mars", "Jupiter"));

        // --- Arabic translations ------------------------------------------------
        bank.add(q("ما هي عاصمة المغرب؟", "استخدم الخريطة.", "الرباط",
                Category.MOROCCAN_HISTORY, 1, Language.AR, "الدار البيضاء", "فاس", "مراكش", "الرباط"));
        bank.add(q("ما هو أكبر محيط يحد المغرب غربا؟", "فكر في محيط كبير.",
                "المحيط الأطلسي", Category.GEOGRAPHY, 1, Language.AR,
                "البحر الأحمر", "المحيط الأطلسي", "البحر الأبيض المتوسط", "المحيط الهندي"));
        bank.add(q("من كتب مسرحية روميو وجولييت؟", "كاتب مسرحي إنجليزي.",
                "وليام شكسبير", Category.GENERAL_KNOWLEDGE, 1, Language.AR,
                "تشارلز ديكنز", "وليام شكسبير", "جين أوستن", "أوسكار وايلد"));

        // --- Arabic: Arabic and Islamic culture plus wider coverage -------------
        bank.add(q("ما هي اللغة الأكثر انتشارا في العالم العربي؟", "إنها لغة القرآن.",
                "العربية", Category.ARABIC_CULTURE, 1, Language.AR,
                "الفارسية", "التركية", "العربية", "الكردية"));
        bank.add(q("في أي مدينة يقع مقر جامعة الدول العربية؟", "إنها عاصمة مصر.",
                "القاهرة", Category.ARABIC_CULTURE, 2, Language.AR,
                "الرياض", "القاهرة", "عمّان", "تونس"));
        bank.add(q("ما هي المدينة اليمنية القديمة الملقبة بـ 'مانهاتن الصحراء'؟",
                "بيوتها الطينية مدرجة في قائمة التراث العالمي.", "شبام",
                Category.ARABIC_CULTURE, 3, Language.AR, "صنعاء", "عدن", "شبام", "تعز"));
        bank.add(q("كم عدد أركان الإسلام؟", "منها الصلاة والصيام.",
                "خمسة", Category.ISLAMIC_CULTURE, 1, Language.AR,
                "ثلاثة", "أربعة", "خمسة", "ستة"));
        bank.add(q("إلى أي مدينة يتجه المسلمون في صلاتهم؟", "تقع في السعودية.",
                "مكة المكرمة", Category.ISLAMIC_CULTURE, 2, Language.AR,
                "المدينة المنورة", "مكة المكرمة", "القدس", "دمشق"));
        bank.add(q("من هو العالم المسلم الذي ألّف كتاب 'القانون في الطب'؟",
                "عاش في بلاد فارس خلال العصر الذهبي للإسلام.", "ابن سينا",
                Category.ISLAMIC_CULTURE, 3, Language.AR, "ابن سينا", "ابن خلدون", "الخوارزمي", "الرازي"));
        bank.add(q("ما هو أكبر محيط في العالم؟", "يفصل آسيا عن الأمريكتين.",
                "المحيط الهادئ", Category.GEOGRAPHY, 1, Language.AR,
                "المحيط الأطلسي", "المحيط الهادئ", "المحيط الهندي", "المحيط المتجمد الشمالي"));
        bank.add(q("ما هو أقرب كوكب إلى الشمس؟", "إنه أصغر من الأرض.",
                "عطارد", Category.SCIENCE, 2, Language.AR,
                "الزهرة", "عطارد", "المريخ", "المشتري"));

        return bank;
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
        question.setOptions(List.of(options));
        return question;
    }
}
