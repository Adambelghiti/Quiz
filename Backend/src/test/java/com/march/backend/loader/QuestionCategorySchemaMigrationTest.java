package com.march.backend.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces the upgrade path that matters for an existing H2 development
 * database: adding a Java enum value does not update an existing H2 ENUM
 * column, so the migration has to widen the stored type before seeding.
 */
class QuestionCategorySchemaMigrationTest {

    private static final String OLD_CATEGORIES =
            "'ARABIC_CULTURE','GENERAL_KNOWLEDGE','GEOGRAPHY','ISLAMIC_CULTURE',"
                    + "'MOROCCAN_CULTURE','MOROCCAN_HISTORY','MUSIC','SCIENCE','SPORTS'";

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MySQL",
                "sa",
                "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE QUESTIONS (CATEGORY ENUM(" + OLD_CATEGORIES + "))");
        jdbc.execute("CREATE TABLE GAMES (CATEGORY ENUM(" + OLD_CATEGORIES + "))");
    }

    @Test
    void addsNewCategoryValuesToExistingQuestionAndGameColumns() {
        QuestionCategorySchemaMigration migration = new QuestionCategorySchemaMigration(jdbc);

        migration.run();
        migration.run();

        jdbc.update("INSERT INTO QUESTIONS (CATEGORY) VALUES ('TECHNOLOGY')");
        jdbc.update("INSERT INTO QUESTIONS (CATEGORY) VALUES ('EUROPEAN_HISTORY')");
        jdbc.update("INSERT INTO QUESTIONS (CATEGORY) VALUES ('SCIENTISTS_AND_INNOVATIONS')");
        jdbc.update("INSERT INTO GAMES (CATEGORY) VALUES ('ART_AND_ARCHITECTURE')");

        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM QUESTIONS WHERE CATEGORY = 'TECHNOLOGY'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM QUESTIONS WHERE CATEGORY = 'EUROPEAN_HISTORY'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM QUESTIONS WHERE CATEGORY = 'SCIENTISTS_AND_INNOVATIONS'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM GAMES WHERE CATEGORY = 'ART_AND_ARCHITECTURE'", Integer.class));
    }
}
