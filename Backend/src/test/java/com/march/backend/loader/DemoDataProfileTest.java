package com.march.backend.loader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demo accounts and the sample question bank use well-known credentials. They
 * must never be created in a production deployment, where the first visitor
 * could otherwise sign in as an administrator.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prod-profile-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=production-profile-test-secret-that-is-long-enough"
})
@ActiveProfiles("prod")
class DemoDataProfileTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void theDemoSeederIsNotLoadedOutsideDevelopmentProfiles() {
        assertEquals(0, context.getBeanNamesForType(DataLoader.class).length,
                "production must not create the demo and admin accounts");
    }
}
