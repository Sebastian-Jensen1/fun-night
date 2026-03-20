package com.funnight.backend.service;

import com.funnight.backend.model.Player;
import com.funnight.backend.model.Session;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SessionService {
    private final Map<Long, Session> sessions = new HashMap<>();
    private final AtomicLong sessionCounter = new AtomicLong(1);
    private final AtomicLong playerCounter = new AtomicLong(1);

    /**
     * Creates a new game session.
     */
    public Session createSession() {
        Long sessionId = sessionCounter.getAndIncrement();
        Session session = new Session(sessionId);
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * Retrieves the session with the given ID.
     */
    public Optional<Session> getSession(Long sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Adds a player to a specific session.
     */
    public Player addPlayerToSession(Long sessionId, String username) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist.");
        }

        Long playerId = playerCounter.getAndIncrement();
        Player player = new Player(playerId, username);
        session.addPlayer(player);
        return player;
    }
    
    /**
     * Removes a player from a specific session by their username.
     */
    public void removePlayerFromSession(Long sessionId, String username) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist.");
        }

        boolean removed = session.getPlayers().removeIf(player -> player.getUsername().equals(username));
        if (!removed) {
            throw new IllegalArgumentException("Player with username " + username + " does not exist in session " + sessionId);
        }
    }

    /**
     * Returns all active sessions.
     */
    public Map<Long, Session> getAllSessions() {
        return sessions;
    }
}
