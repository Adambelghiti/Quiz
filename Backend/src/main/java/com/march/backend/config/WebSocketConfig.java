package com.march.backend.config;

import com.march.backend.security.StompAuthChannelInterceptor;
import com.march.backend.security.StompGameBroadcastInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * STOMP-over-WebSocket configuration.
 * <p>
 * Clients connect to {@code /ws} (sending their JWT in the STOMP
 * {@code Authorization} native header) and subscribe to:
 * <ul>
 *   <li>{@code /topic/lobby} – the list of open games</li>
 *   <li>{@code /topic/game/{id}} – live state of a single game (participants only)</li>
 * </ul>
 * The server only pushes updates; commands continue to go through the REST API
 * (so transactions, validation and security stay in one place).
 * <p>
 * The handshake only accepts the same origins as the REST API
 * ({@code app.cors.allowed-origins}); a wildcard would let any website open a
 * WebSocket against the server on behalf of a logged-in user.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final StompGameBroadcastInterceptor stompGameBroadcastInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
                           StompGameBroadcastInterceptor stompGameBroadcastInterceptor,
                           @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:4300}") String allowedOrigins) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.stompGameBroadcastInterceptor = stompGameBroadcastInterceptor;
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
    }

    /** Split the comma-separated {@code app.cors.allowed-origins} property. */
    static String[] parseAllowedOrigins(String allowedOrigins) {
        if (allowedOrigins == null) {
            return new String[0];
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompGameBroadcastInterceptor);
    }
}
