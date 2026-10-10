package com.funnight.backend.session;

import com.funnight.backend.exception.CapacityExceededException;
import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.exception.InvalidRequestException;
import com.funnight.backend.exception.InvalidSessionTokenException;
import com.funnight.backend.exception.SessionNotFoundException;
import com.funnight.backend.game.GameType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionServiceTest {
    private final InMemorySessionStore store = new InMemorySessionStore();

    private SessionService service(Clock clock, int max) {
        return new SessionService(store, clock, max, Duration.ofHours(6));
    }

    private SessionService service() {
        return service(Clock.systemUTC(), 100);
    }

    @Test
    void fullSessionWalksThroughEveryPlayableGameAndSkipsTheRest() {
        SessionService service = service();
        CreateSessionResponse created = service.create(
                new CreateSessionRequest(SessionMode.FULL, null, List.of("Seb", "Anna")));
        String id = created.session().id();
        String token = created.token();

        assertEquals(6, created.session().games().size());
        assertEquals("blackjack", created.session().currentGame().id());

        SessionView view = service.advance(id, token);
        assertEquals("dares", view.currentGame().id());

        long playable = List.of(GameType.values()).stream().filter(GameType::isAvailable).count();
        for (int i = 2; i < playable; i++) view = service.advance(id, token);
        view = service.advance(id, token);
        assertTrue(view.finished());
        assertNull(view.currentGame());
        assertThrows(GameRuleException.class, () -> service.advance(id, token));
    }

    @Test
    void quickplayHasOneGame() {
        SessionView view = service().create(
                new CreateSessionRequest(SessionMode.QUICKPLAY, "blackjack", List.of("Seb"))).session();
        assertEquals(1, view.games().size());
        assertEquals("blackjack", view.currentGame().id());
    }

    @Test
    void quickplayRejectsUnknownOrUnavailableGames() {
        SessionService service = service();
        assertThrows(InvalidRequestException.class, () -> service.create(
                new CreateSessionRequest(SessionMode.QUICKPLAY, "nope", List.of("Seb"))));
        assertThrows(InvalidRequestException.class, () -> service.create(
                new CreateSessionRequest(SessionMode.QUICKPLAY, "game-6", List.of("Seb"))));
    }

    @Test
    void namesAreCleanedAndMustBeUnique() {
        SessionService service = service();
        SessionView view = service.create(
                new CreateSessionRequest(SessionMode.FULL, null, List.of("  Seb   J ", "Anna"))).session();
        assertEquals("Seb J", view.players().get(0).name());

        assertThrows(InvalidRequestException.class, () -> service.create(
                new CreateSessionRequest(SessionMode.FULL, null, List.of("Seb", "seb"))));
        assertThrows(InvalidRequestException.class, () -> service.create(
                new CreateSessionRequest(SessionMode.FULL, null, List.of("<script>"))));
    }

    @Test
    void wrongTokenIsRejected() {
        SessionService service = service();
        String id = service.create(new CreateSessionRequest(SessionMode.FULL, null, List.of("Seb"))).session().id();

        assertThrows(InvalidSessionTokenException.class, () -> service.get(id, "wrong"));
        assertThrows(InvalidSessionTokenException.class, () -> service.get(id, null));
        assertThrows(SessionNotFoundException.class, () -> service.get("missing", "whatever"));
    }

    @Test
    void capacityIsEnforced() {
        SessionService service = service(Clock.systemUTC(), 1);
        service.create(new CreateSessionRequest(SessionMode.FULL, null, List.of("Seb")));
        assertThrows(CapacityExceededException.class, () ->
                service.create(new CreateSessionRequest(SessionMode.FULL, null, List.of("Seb"))));
    }

    @Test
    void idleSessionsAreCleanedUp() {
        Instant start = Instant.parse("2026-01-01T20:00:00Z");
        SessionService creator = service(Clock.fixed(start, ZoneOffset.UTC), 100);
        creator.create(new CreateSessionRequest(SessionMode.FULL, null, List.of("Seb")));

        service(Clock.fixed(start.plus(Duration.ofHours(5)), ZoneOffset.UTC), 100).removeIdleSessions();
        assertEquals(1, store.count());
        service(Clock.fixed(start.plus(Duration.ofHours(7)), ZoneOffset.UTC), 100).removeIdleSessions();
        assertEquals(0, store.count());
    }
}
