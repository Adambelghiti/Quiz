package com.march.backend.security;

import com.march.backend.model.Player;
import com.march.backend.repository.GameRepository;
import com.march.backend.service.PlayerService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Re-checks game membership for every outbound game message.
 * <p>
 * Authorizing only the {@code SUBSCRIBE} frame is not enough: a player who is
 * removed from a room keeps the broker subscription they opened earlier and
 * would continue to receive the shared topic. The outbound channel sees each
 * message that is about to be delivered to a session, so dropping it here
 * closes that gap without having to disconnect the socket.
 */
@Component
public class StompGameBroadcastInterceptor implements ChannelInterceptor {

    private static final String GAME_TOPIC_PREFIX = "/topic/game/";

    private final PlayerService playerService;
    private final GameRepository gameRepository;
    private final StompSessionRegistry sessionRegistry;

    public StompGameBroadcastInterceptor(PlayerService playerService,
                                         GameRepository gameRepository,
                                         StompSessionRegistry sessionRegistry) {
        this.playerService = playerService;
        this.gameRepository = gameRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(GAME_TOPIC_PREFIX)) {
            return message;
        }

        Long gameId = parseGameId(destination);
        if (gameId == null) {
            return null;
        }

        Optional<String> username = sessionRegistry.usernameFor(accessor.getSessionId());
        if (username.isEmpty()) {
            return null;
        }

        Optional<Player> player = playerService.findForUser(username.get());
        if (player.isEmpty()) {
            return null;
        }

        return gameRepository.countPlayersInGame(gameId, player.get().getId()) > 0
                ? message
                : null;
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
}
