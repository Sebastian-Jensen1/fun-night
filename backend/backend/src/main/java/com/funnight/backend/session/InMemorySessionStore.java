package com.funnight.backend.session;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionStore implements SessionStore {
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(GameSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public Optional<GameSession> findById(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public void delete(String id) {
        sessions.remove(id);
    }

    @Override
    public int count() {
        return sessions.size();
    }

    @Override
    public int removeIdleSince(Instant cutoff) {
        int before = sessions.size();
        sessions.values().removeIf(s -> s.getLastActivity().isBefore(cutoff));
        return before - sessions.size();
    }
}
