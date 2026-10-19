import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Additive upsert seeder for the question bank.
 *
 * <p>Reads a questions file (default {@code ../new_questions.json}) and inserts every
 * (question, language) pair that is not already present, matching on (text, language)
 * exactly like {@code DataLoader}. Existing rows are never updated or deleted, so this
 * is safe to run repeatedly and never wipes the current bank.
 *
 * <p>Run it from the {@code Backend} directory with the project classpath, e.g.:
 * <pre>
 *   mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target/cp.txt"
 *   java -cp "target/classes;$(cat target/cp.txt)" tools/SeedNewQuestions.java
 * </pre>
 *
 * <p>Optional arguments: {@code [questions-json] [jdbc-url]}.
 */
public class SeedNewQuestions {

    private static final List<String> LANGUAGES = List.of("EN", "FR", "AR");
    private static final String DEFAULT_URL = "jdbc:h2:file:./data/quizdb;AUTO_SERVER=TRUE;MODE=MySQL";

    public static void main(String[] args) throws Exception {
        Path jsonFile = Path.of(args.length > 0 ? args[0] : "../new_questions.json").toAbsolutePath();
        String url = args.length > 1 ? args[1] : DEFAULT_URL;

        if (!Files.isReadable(jsonFile)) {
            System.err.println("Cannot read questions file: " + jsonFile);
            System.exit(2);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode questions = mapper.readTree(Files.readString(jsonFile)).path("questions");
        if (!questions.isArray()) {
            System.err.println("Expected a top-level 'questions' array in " + jsonFile);
            System.exit(2);
        }

        int inserted = 0;
        int skipped = 0;
        int malformed = 0;

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            connection.setAutoCommit(false);
            int before = count(connection);

            for (JsonNode question : questions) {
                String category = question.path("category").asText();
                int difficulty = question.path("difficulty").asInt(1);
                JsonNode translations = question.path("translations");

                for (String language : LANGUAGES) {
                    JsonNode translation = translations.path(language);
                    if (translation.isMissingNode()) {
                        continue;
                    }

                    String text = translation.path("text").asText();
                    String hint = translation.path("hint").asText(null);
                    String explanation = translation.path("explanation").asText(null);
                    List<String> options = new ArrayList<>();
                    translation.path("options").forEach(option -> options.add(option.asText()));
                    int correctIndex = translation.path("correctIndex").asInt(-1);

                    if (text.isBlank() || options.size() != 4 || correctIndex < 0 || correctIndex >= options.size()) {
                        System.err.printf("Skipping malformed entry (%s/%s): %s%n", category, language, text);
                        malformed++;
                        continue;
                    }

                    if (exists(connection, text, language)) {
                        skipped++;
                        continue;
                    }

                    String correctAnswer = options.get(correctIndex);
                    long id = insertQuestion(connection, text, category, difficulty, language, correctAnswer,
                            hint, explanation);
                    insertOptions(connection, id, options);
                    inserted++;
                }
            }
            connection.commit();

            System.out.printf("Database: %s%n", url);
            System.out.printf("Question rows before: %d, after: %d%n", before, count(connection));
            System.out.printf("Inserted %d row(s), skipped %d already present, %d malformed.%n",
                    inserted, skipped, malformed);
        }
    }

    private static int count(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM questions")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean exists(Connection connection, String text, String language) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM questions WHERE text = ? AND language = ?")) {
            ps.setString(1, text);
            ps.setString(2, language);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static long insertQuestion(Connection connection, String text, String category, int difficulty,
                                       String language, String correctAnswer, String hint,
                                       String explanation) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO questions (text, category, difficulty, language, correct_answer, hint, explanation) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, text);
            ps.setString(2, category);
            ps.setInt(3, difficulty);
            ps.setString(4, language);
            ps.setString(5, correctAnswer);
            ps.setString(6, hint);
            ps.setString(7, explanation);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void insertOptions(Connection connection, long questionId, List<String> options) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO question_options (question_id, option_text) VALUES (?, ?)")) {
            for (String option : options) {
                ps.setLong(1, questionId);
                ps.setString(2, option);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
