package com.march.backend.security;

import com.march.backend.model.AuthUser;
import com.march.backend.model.Game;
import com.march.backend.model.GameStatus;
import com.march.backend.model.Player;
import com.march.backend.repository.AuthUserRepository;
import com.march.backend.repository.GameRepository;
import com.march.backend.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the STOMP authorization rules: only authenticated sessions may
 * subscribe at all, and game topics are limited to that game's participants.
 * <p>
 * The interceptor is driven directly with synthetic STOMP frames, so no real
 * WebSocket connection is needed; the repositories and {@link JwtService} are
 * the real ones.
 */
@SpringBootTest
@ActiveProfiles("test")
class StompAuthChannelInterceptorTest {

    @Autowired
    private StompAuthChannelInterceptor interceptor;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthUserRepository authUserRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private GameRepository gameRepository;

    @Test
    void anonymousSubscriptionIsRejected() {
        StompHeaderAccessor accessor = subscribeTo("/topic/lobby", null);

        assertThrows(MessagingException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void connectWithAValidTokenAttachesAPrincipal() {
        String username = "stomp-connect-user";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer " + jwtService.generateToken(username, "USER"));

        interceptor.preSend(message(accessor), null);

        assertNotNull(accessor.getUser(), "a valid token should attach a principal");
        assertEquals(username, accessor.getUser().getName());
    }

    @Test
    void connectWithoutATokenStaysAnonymous() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);

        interceptor.preSend(message(accessor), null);

        assertNull(accessor.getUser());
    }

    @Test
    void participantsCanSubscribeToTheirGameTopic() {
        Player player = newPlayer("stomp-participant");
        Game game = newGame(player);

        StompHeaderAccessor accessor = subscribeTo("/topic/game/" + game.getId(), player.getName());

        assertDoesNotThrow(() -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void outsidersCannotSubscribeToAGameTopic() {
        Player participant = newPlayer("stomp-insider");
        Player outsider = newPlayer("stomp-outsider");
        Game game = newGame(participant);

        StompHeaderAccessor accessor = subscribeTo("/topic/game/" + game.getId(), outsider.getName());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void accountWithoutAPlayerRowCannotSubscribeToAGameTopic() {
        Player participant = newPlayer("stomp-host");
        Game game = newGame(participant);

        AuthUser accountOnly = new AuthUser();
        accountOnly.setUsername("stomp-account-only");
        accountOnly.setPassword("not-used-in-tests");
        accountOnly.setRole("USER");
        authUserRepository.save(accountOnly);

        StompHeaderAccessor accessor = subscribeTo("/topic/game/" + game.getId(), accountOnly.getUsername());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message(accessor), null));
    }

    /* ------------------------------------------------------------------ */

    private StompHeaderAccessor subscribeTo(String destination, String username) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (username != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
        }
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    /** Create an account together with the in-game player linked to it. */
    private Player newPlayer(String username) {
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword("not-used-in-tests");
        user.setRole("USER");
        AuthUser saved = authUserRepository.save(user);

        Player player = new Player();
        player.setName(username);
        player.setAuthUserId(saved.getId());
        return playerRepository.save(player);
    }

    private Game newGame(Player... players) {
        Game game = new Game();
        game.setStatus(GameStatus.IN_PROGRESS);
        for (Player player : players) {
            game.getPlayers().add(player);
        }
        if (players.length > 0) {
            game.setHostPlayerId(players[0].getId());
        }
        return gameRepository.save(game);
    }
}
