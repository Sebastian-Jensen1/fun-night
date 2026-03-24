package com.funnight.backend.service;

import com.funnight.backend.model.Player;
import com.funnight.backend.model.Round;
import com.funnight.backend.model.Session;
import com.funnight.backend.model.Challenge;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AtomicLong roundCounter = new AtomicLong(1);
    private final ChallengeService challengeService;

    @Autowired
    public SessionService(ChallengeService challengeService) {
        this.challengeService = challengeService;}

    /** Creates a new game session.*/
    public Session createSession() {
        Long sessionId = sessionCounter.getAndIncrement();
        Session session = new Session(sessionId);
        sessions.put(sessionId, session);
        return session;
    }

    /** Retrieves the session with the given ID. */
    public Optional<Session> getSession(Long sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /** Adds a player to a specific session. */
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
    
    /** Removes a player from a specific session by their username. */
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

    /** Returns all active sessions. */
    public Map<Long, Session> getAllSessions() {
        return sessions;
    }

    public Round startNewRound (Long sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session with ID " + sessionId + " does not exist.");
        }
        Map<Long,Challenge> assignments = new HashMap<>();
        for (Player player : session.getPlayers()) {
            Challenge challenge = challengeService.getRandomChallenge();
            assignments.put(player.getPlayerId(),challenge);

        }
        Long roundId = roundCounter.getAndIncrement();
        Round newRound = new Round(roundId, assignments);
        if (session.getPlayers().isEmpty()) { throw new IllegalArgumentException("Cannot start a round with no players"); }
        session.getRounds().add(newRound);
        return newRound;
    }
}
