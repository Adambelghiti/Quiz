package com.march.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoder used by both the auth controller and the data seeder.
 * Kept in its own configuration so it is available independently of the
 * {@link com.march.backend.config.SecurityConfig} filter chain (for example in
 * slice tests that only load part of the context).
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
