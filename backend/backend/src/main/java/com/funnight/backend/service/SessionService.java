package com.funnight.backend.service;

import com.funnight.backend.exception.EmptySessionException;
import com.funnight.backend.exception.PlayerAlreadyExistException;
import com.funnight.backend.exception.PlayerNotFoundException;
import com.funnight.backend.exception.SessionNotFoundException;
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
            throw new SessionNotFoundException(sessionId);
        }
        boolean duplicate = session.getPlayers().stream()
                .anyMatch(player -> player.getUsername().equals(username));
        if (duplicate) {
            throw new PlayerAlreadyExistException(username, sessionId);
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
            throw new SessionNotFoundException(sessionId);
        }

        boolean removed = session.getPlayers().removeIf(player -> player.getUsername().equals(username));
        if (!removed) {
            throw new PlayerNotFoundException(username, sessionId);
        }
    }
    /** Edit username for a player*/
    public void editUsername(Long sessionId, String oldUsername, String newUsername) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new  SessionNotFoundException(sessionId);
        }

        Player player = session.getPlayers().stream()
                .filter(p -> p.getUsername().equals(oldUsername))
                .findFirst()
                .orElseThrow(() -> new PlayerNotFoundException(oldUsername, sessionId));

        player.setUsername(newUsername);
    }

    /** Returns all active sessions. */
    public Map<Long, Session> getAllSessions()  {return sessions;}

    /** Start a new round*/
    public Round startNewRound (Long sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException(sessionId);
        }
        Map<Long,Challenge> assignments = new HashMap<>();
        for (Player player : session.getPlayers()) {
            Challenge challenge = challengeService.getRandomChallenge();
            assignments.put(player.getPlayerId(),challenge);

        }
        Long roundId = roundCounter.getAndIncrement();
        Round round = new Round(roundId, assignments);
        if (session.getPlayers().isEmpty()) { throw new EmptySessionException(sessionId,roundId); }
        session.getRounds().add(round);
        return round;
    }
}
