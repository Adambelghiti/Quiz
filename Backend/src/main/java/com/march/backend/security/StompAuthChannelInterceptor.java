package com.march.backend.security;

import com.march.backend.model.Game;
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
 * validated and attached as the session principal. On {@code SUBSCRIBE} to a
 * specific game topic ({@code /topic/game/{id}}) the principal must be a player
 * in that game, so nobody can watch another game's live state.
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

    /** Reject subscriptions to game topics the principal is not part of. */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(GAME_TOPIC_PREFIX)) {
            return;
        }
        if (accessor.getUser() == null || accessor.getUser().getName() == null) {
            throw new MessagingException("Authentication required to subscribe to game topics");
        }

        Long gameId = parseGameId(destination);
        if (gameId == null) {
            throw new MessagingException("Invalid game topic: " + destination);
        }

        Long playerId = playerService.resolveForUser(accessor.getUser().getName()).getId();
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new MessagingException("Game not found: " + gameId));
        boolean participant = game.getPlayers().stream()
                .anyMatch(p -> p.getId().equals(playerId));
        if (!participant) {
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
