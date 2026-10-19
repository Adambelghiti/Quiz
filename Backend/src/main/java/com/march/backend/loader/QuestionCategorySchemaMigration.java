package com.march.backend.loader;

import com.march.backend.model.Category;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Keeps an existing H2 development database usable when a new {@link Category}
 * value is added.
 * <p>
 * Hibernate creates the category columns as H2 {@code ENUM} types. Adding a
 * Java enum value does not alter that stored type, and {@code ddl-auto=update}
 * will not widen it either, so seeding a new category fails with "value not
 * permitted". This migration widens the category columns before
 * {@link DataLoader} runs. Production deployments should use a real migration
 * tool instead.
 */
@Component
@Profile({"dev", "local"})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class QuestionCategorySchemaMigration implements CommandLineRunner {

    private static final List<String> CATEGORY_TABLES = List.of("QUESTIONS", "GAMES");

    private final JdbcTemplate jdbc;

    public QuestionCategorySchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        if (!isH2()) {
            return;
        }
        for (String table : CATEGORY_TABLES) {
            widenCategoryColumn(table);
        }
    }

    private boolean isH2() {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())));
    }

    private void widenCategoryColumn(String table) {
        String dataType = jdbc.query(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = ? AND COLUMN_NAME = 'CATEGORY'",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null,
                table);
        if (!"ENUM".equalsIgnoreCase(dataType)) {
            return;
        }

        List<String> storedValues = jdbc.queryForList(
                "SELECT VALUE_NAME FROM INFORMATION_SCHEMA.ENUM_VALUES WHERE OBJECT_NAME = ?",
                String.class,
                table);
        List<String> expectedValues = Arrays.stream(Category.values())
                .map(Category::name)
                .toList();
        if (storedValues.containsAll(expectedValues)) {
            return;
        }

        String enumValues = expectedValues.stream()
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(","));
        try {
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN CATEGORY ENUM(" + enumValues + ")");
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Could not add the new question categories to " + table + ".CATEGORY", ex);
        }
    }
}
