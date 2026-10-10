package com.funnight.backend.session;

import java.time.Instant;
import java.util.Optional;

/**
 * Storage for live sessions. The default implementation keeps them in memory; swap in a shared
 * store (Redis, a database, ...) to run several backend instances behind a load balancer.
 */
public interface SessionStore {
    void save(GameSession session);
    Optional<GameSession> findById(String id);
    void delete(String id);
    int count();
    /** Removes sessions with no activity since {@code cutoff}; returns how many were removed. */
    int removeIdleSince(Instant cutoff);
}
