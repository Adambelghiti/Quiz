package com.march.backend.loader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A fresh checkout starts with no explicit profile. The configured default
 * profile must make that local development mode work, including the bundled
 * seed data and the development JWT placeholder.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:default-profile-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DefaultProfileTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void theDefaultProfileLoadsTheDevelopmentSeeder() {
        assertEquals(1, context.getBeanNamesForType(DataLoader.class).length);
    }
}
