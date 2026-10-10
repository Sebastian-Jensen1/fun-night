package com.funnight.backend.session;

import com.funnight.backend.exception.CapacityExceededException;
import com.funnight.backend.exception.InvalidRequestException;
import com.funnight.backend.exception.InvalidSessionTokenException;
import com.funnight.backend.exception.SessionNotFoundException;
import com.funnight.backend.game.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

@Service
public class SessionService {
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 8;
    public static final int MAX_NAME_LENGTH = 20;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} _.'!-]{1," + MAX_NAME_LENGTH + "}$");

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionStore store;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int maxSessions;
    private final Duration idleTimeout;

    public SessionService(SessionStore store,
                          Clock clock,
                          @Value("${funnight.sessions.max-active:5000}") int maxSessions,
                          @Value("${funnight.sessions.idle-timeout:PT6H}") Duration idleTimeout) {
        this.store = store;
        this.clock = clock;
        this.maxSessions = maxSessions;
        this.idleTimeout = idleTimeout;
    }

    public CreateSessionResponse create(CreateSessionRequest request) {
        List<GameType> lineUp = lineUpFor(request);
        List<Player> players = buildPlayers(request.playerNames());

        if (store.count() >= maxSessions) {
            throw new CapacityExceededException();
        }

        String token = newToken();
        GameSession session = new GameSession(UUID.randomUUID().toString(), hash(token), request.mode(),
                players, lineUp, clock.instant());
        store.save(session);
        return new CreateSessionResponse(SessionView.of(session), token);
    }

    /**
     * Looks up a session, checks the caller's token and runs {@code action} while holding the
     * session's lock, so concurrent requests for the same table can't corrupt game state.
     */
    public <T> T withSession(String sessionId, String token, Function<GameSession, T> action) {
        GameSession session = store.findById(sessionId).orElseThrow(SessionNotFoundException::new);
        if (token == null || !MessageDigest.isEqual(session.getTokenHash(), hash(token))) {
            throw new InvalidSessionTokenException();
        }
        synchronized (session) {
            session.touch(clock.instant());
            return action.apply(session);
        }
    }

    public SessionView get(String sessionId, String token) {
        return withSession(sessionId, token, SessionView::of);
    }

    public SessionView advance(String sessionId, String token) {
        return withSession(sessionId, token, session -> {
            session.advance();
            return SessionView.of(session);
        });
    }

    public void end(String sessionId, String token) {
        withSession(sessionId, token, session -> {
            store.delete(session.getId());
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${funnight.sessions.cleanup-interval-ms:300000}")
    public void removeIdleSessions() {
        int removed = store.removeIdleSince(clock.instant().minus(idleTimeout));
        if (removed > 0) {
            log.info("Removed {} idle session(s)", removed);
        }
    }

    private static List<GameType> lineUpFor(CreateSessionRequest request) {
        if (request.mode() == SessionMode.FULL) {
            return Arrays.asList(GameType.values());
        }
        GameType game = GameType.fromId(request.gameId() == null ? "" : request.gameId())
                .orElseThrow(() -> new InvalidRequestException("Pick a game for quickplay."));
        if (!game.isAvailable()) {
            throw new InvalidRequestException(game.getDisplayName() + " is not playable yet.");
        }
        return List.of(game);
    }

    private static List<Player> buildPlayers(List<String> rawNames) {
        List<Player> players = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : rawNames) {
            String name = raw == null ? "" : raw.strip().replaceAll("\\s+", " ");
            if (!NAME_PATTERN.matcher(name).matches()) {
                throw new InvalidRequestException("Names may only use letters, numbers, spaces and _ . ' ! - (max "
                        + MAX_NAME_LENGTH + " characters).");
            }
            if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                throw new InvalidRequestException("Two players can't both be called \"" + name + "\".");
            }
            players.add(new Player(players.size() + 1, name));
        }
        return players;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
