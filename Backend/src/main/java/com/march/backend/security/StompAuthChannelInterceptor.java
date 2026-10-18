package com.march.backend.security;

import com.march.backend.model.Player;
import com.march.backend.repository.GameRepository;
import com.march.backend.service.PlayerService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates STOMP connections and scopes subscriptions.
 * <p>
 * On {@code CONNECT} the JWT from the {@code Authorization} native header is
 * validated and attached as the session principal. Every {@code SUBSCRIBE} then
 * requires an authenticated principal, and subscriptions to a specific game
 * topic ({@code /topic/game/{id}}) additionally require the principal to be a
 * player in that game, so anonymous clients or outsiders can never watch live
 * game state.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String GAME_TOPIC_PREFIX = "/topic/game/";

    private final JwtService jwtService;
    private final PlayerService playerService;
    private final GameRepository gameRepository;

    public StompAuthChannelInterceptor(JwtService jwtService,
                                       PlayerService playerService,
                                       GameRepository gameRepository) {
        this.jwtService = jwtService;
        this.playerService = playerService;
        this.gameRepository = gameRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String username = authenticate(accessor);
            if (username != null) {
                accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    /** Validate the bearer token and return the username, or {@code null}. */
    private String authenticate(StompHeaderAccessor accessor) {
        String header = firstHeader(accessor, "Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            String username = jwtService.extractUsername(token);
            return jwtService.isValid(token, username) ? username : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Reject subscriptions from anonymous sessions, and game-topic
     * subscriptions from principals that are not part of that game.
     */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null || accessor.getUser().getName() == null) {
            throw new MessagingException("Authentication required to subscribe to topics");
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(GAME_TOPIC_PREFIX)) {
            return;
        }

        Long gameId = parseGameId(destination);
        if (gameId == null) {
            throw new MessagingException("Invalid game topic: " + destination);
        }

        Long playerId = playerService.findForUser(accessor.getUser().getName())
                .map(Player::getId)
                .orElseThrow(() -> new MessagingException("You are not a player in game " + gameId));
        // Count query instead of loading the game: this runs outside a
        // transaction, so the lazy `players` collection cannot be initialised.
        if (gameRepository.countPlayersInGame(gameId, playerId) == 0) {
            throw new MessagingException("You are not a player in game " + gameId);
        }
    }

    private Long parseGameId(String destination) {
        String tail = destination.substring(GAME_TOPIC_PREFIX.length());
        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }
        try {
            return Long.valueOf(tail.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return accessor.getFirstNativeHeader(name);
    }
}
