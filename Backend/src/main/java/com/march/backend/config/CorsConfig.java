package com.march.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration for the REST API.
 * <p>
 * The Angular dev server runs on a different origin (by default
 * {@code http://localhost:4200}) than the backend ({@code http://localhost:8080}),
 * so every API call is a cross-origin request. Instead of annotating each
 * controller with {@code @CrossOrigin}, a single {@link CorsConfigurationSource}
 * bean is registered here and picked up by Spring Security via
 * {@code http.cors(...)}.
 */
@Configuration
public class CorsConfig {

    /** Origins allowed to call the API, configurable through application.properties. */
    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:4300}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
