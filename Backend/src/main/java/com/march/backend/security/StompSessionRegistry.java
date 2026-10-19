package com.march.backend.security;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the authenticated username for each live STOMP session.
 * <p>
 * Outbound messages are addressed to a session id, not to a principal, so the
 * broker-side authorization check needs this small mapping. Entries are removed
 * when the client disconnects.
 */
@Component
public class StompSessionRegistry {

    private final Map<String, String> usernamesBySession = new ConcurrentHashMap<>();

    public void register(String sessionId, String username) {
        if (sessionId != null && username != null) {
            usernamesBySession.put(sessionId, username);
        }
    }

    public void unregister(String sessionId) {
        if (sessionId != null) {
            usernamesBySession.remove(sessionId);
        }
    }

    public Optional<String> usernameFor(String sessionId) {
        return sessionId == null
                ? Optional.empty()
                : Optional.ofNullable(usernamesBySession.get(sessionId));
    }

    /** Also clean up after an abrupt socket close that skipped STOMP DISCONNECT. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        unregister(event.getSessionId());
    }
}
