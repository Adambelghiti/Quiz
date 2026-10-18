package com.march.backend.service;

import com.march.backend.dto.GameSummary;
import com.march.backend.dto.RoundResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pushes live game state to subscribed clients over STOMP.
 * <p>
 * Note: messages are sent from inside the (transactional) service methods, so a
 * client may briefly observe state slightly before the commit completes. For a
 * demo this is acceptable; a production system would publish after commit via
 * {@code TransactionSynchronization}.
 */
@Service
public class GameEventPublisher {

    private final SimpMessagingTemplate messaging;

    public GameEventPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public static String gameTopic(Long gameId) {
        return "/topic/game/" + gameId;
    }

    /** Broadcast the list of open games to the lobby. */
    public void publishLobby(List<GameSummary> games) {
        messaging.convertAndSend("/topic/lobby", games);
    }

    /** Broadcast the state of one game to everyone watching it. */
    public void publishGame(Long gameId, RoundResponse state) {
        messaging.convertAndSend(gameTopic(gameId), state);
    }
}
