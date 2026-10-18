package com.march.backend.config;

import com.march.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Stateless, token-based security.
 * <p>
 * {@code /api/auth/**} is public; every other endpoint requires a valid
 * {@code Authorization: Bearer <token>} header. Missing/invalid tokens result in
 * a JSON 401 response produced by {@link #unauthorized}.
 */
@Configuration
@EnableWebSecurity
@Profile("!test") // Disable during unit tests to avoid pulling in full security stack
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/error").permitAll()
                // WebSocket handshake + STOMP endpoint (updates only, no secrets)
                .requestMatchers("/ws", "/ws/**").permitAll()
                // Question administration is restricted to admins.
                .requestMatchers(HttpMethod.GET, "/api/questions/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/questions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/questions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/questions/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(this::unauthorized))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Writes a consistent JSON body for unauthenticated requests instead of an HTML error page. */
    private void unauthorized(HttpServletRequest request,
                              HttpServletResponse response,
                              AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String path = request.getRequestURI() == null
                ? ""
                : request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        String body = String.format(
                "{\"status\":401,\"error\":\"Unauthorized\","
                        + "\"message\":\"Authentication required or token invalid\","
                        + "\"path\":\"%s\"}", path);
        response.getWriter().write(body);
    }
}
