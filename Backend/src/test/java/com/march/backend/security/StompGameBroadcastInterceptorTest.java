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
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The broker keeps an existing subscription alive after a player is removed
 * from a game. Every outbound game message is therefore re-checked against the
 * current player list, so a kicked player cannot keep watching the game.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StompGameBroadcastInterceptorTest {

    @Autowired
    private StompGameBroadcastInterceptor interceptor;
    @Autowired
    private StompSessionRegistry sessionRegistry;
    @Autowired
    private AuthUserRepository authUserRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private GameRepository gameRepository;

    @Test
    void participantsReceiveGameUpdates() {
        Player player = newPlayer("broadcast-participant");
        Game game = newGame(player);
        sessionRegistry.register("session-participant", player.getName());

        assertNotNull(interceptor.preSend(outbound(game.getId(), "session-participant"), null));
    }

    @Test
    void aKickedPlayerStopsReceivingGameUpdatesOnTheExistingSubscription() {
        Player host = newPlayer("broadcast-host");
        Player guest = newPlayer("broadcast-guest");
        Game game = newGame(host, guest);
        sessionRegistry.register("session-guest", guest.getName());

        assertNotNull(interceptor.preSend(outbound(game.getId(), "session-guest"), null));

        game.getPlayers().removeIf(p -> p.getId().equals(guest.getId()));
        gameRepository.saveAndFlush(game);

        assertNull(interceptor.preSend(outbound(game.getId(), "session-guest"), null),
                "the kicked player's old subscription must no longer receive game state");
    }

    @Test
    void anUnregisteredSessionCannotReceiveGameUpdates() {
        Player player = newPlayer("broadcast-unregistered");
        Game game = newGame(player);

        assertNull(interceptor.preSend(outbound(game.getId(), "unknown-session"), null));
    }

    @Test
    void nonGameTopicsAreNotAffected() {
        Player player = newPlayer("broadcast-lobby");
        sessionRegistry.register("session-lobby", player.getName());

        assertNotNull(interceptor.preSend(outboundTo("/topic/lobby", "session-lobby"), null));
    }

    /* ------------------------------------------------------------------ */

    private Message<byte[]> outbound(Long gameId, String sessionId) {
        return outboundTo("/topic/game/" + gameId, sessionId);
    }

    private Message<byte[]> outboundTo(String destination, String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setDestination(destination);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

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
        return gameRepository.saveAndFlush(game);
    }
}
