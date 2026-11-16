package com.funnight.room;

import com.funnight.common.ApiException;
import com.funnight.content.ContentLibrary;
import com.funnight.game.GameType;
import com.funnight.games.Games;
import com.funnight.games.dares.DareGame;
import com.funnight.games.guilty.GuiltyGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {

    /** A clock the test can wind forward. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T20:00:00Z");

        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final Games games = new Games(new ContentLibrary());
    private RoomService service;

    private RoomService newService(String dataDir, int maxRooms) {
        return new RoomService(new InMemoryRoomStore(), new RoomSnapshots(dataDir), new RoomEvents(), games, clock,
                maxRooms, Duration.ofHours(6));
    }

    @BeforeEach
    void setUp() {
        service = newService("", 100);
    }

    private static HttpStatus statusOf(Runnable call) {
        return assertThrows(ApiException.class, call::run).getStatus();
    }

    @Test
    void hostOpensALobbyAndPhonesJoinWithTheCode() {
        RoomService.Joined host = service.create(Room.Mode.FULL, null, List.of("Anna", "  Bo  "));
        assertEquals(Room.Phase.LOBBY, host.room().phase());
        assertEquals(5, host.room().code().length());
        assertEquals("HOST", host.room().you().role());
        assertEquals("Bo", host.room().players().get(1).name(), "names are trimmed");

        RoomService.Joined phone = service.join(host.room().code().toLowerCase(), "Cy", null);
        assertEquals("PLAYER", phone.room().you().role());
        assertEquals(3, phone.room().you().playerId());
        assertNotEquals(host.token(), phone.token());

        RoomView seenByHost = service.view(host.roomId(), host.token());
        assertEquals(3, seenByHost.players().size());
        assertTrue(seenByHost.players().get(2).remote());
        assertFalse(seenByHost.players().get(0).remote());
    }

    @Test
    void rejectsBadNamesDuplicatesAndFullTables() {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(Room.Mode.FULL, null, List.of("<script>"))));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(Room.Mode.FULL, null, List.of("Anna", "anna"))));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(Room.Mode.QUICKPLAY, "nope", List.of())));

        RoomService.Joined host = service.create(Room.Mode.FULL, null,
                List.of("A", "B", "C", "D", "E", "F", "G", "H"));
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.join(host.room().code(), "Ninth", null)));
        assertTrue(service.lookup(host.room().code()).full());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.lookup("ZZZZZ")));
    }

    @Test
    void onlyTheHostRunsTheNightAndPhonesOnlyPlayTheirOwnSeat() {
        RoomService.Joined host = service.create(Room.Mode.QUICKPLAY, "dares", List.of("Anna"));
        RoomService.Joined phone = service.join(host.room().code(), "Bo", null);
        String id = host.roomId();

        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.start(id, phone.token())));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.view(id, "wrong-token")));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.view(id, null)));
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.view("no-such-room", host.token())));

        RoomView started = service.start(id, host.token());
        assertEquals(Room.Phase.PLAYING, started.phase());
        assertEquals("dares", started.currentGame().id());

        // It's Anna's turn (a host-device seat): Bo's phone can't play it, the host can.
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.play(id, phone.token(), GameType.DARES, DareGame.class,
                (game, actor) -> {
                    actor.requireFor(1);
                    game.draw(1, DareGame.Level.MILD);
                })));
        service.play(id, host.token(), GameType.DARES, DareGame.class, (game, actor) -> {
            actor.requireFor(1);
            game.draw(1, DareGame.Level.MILD);
            game.resolve(1, true);
        });
        // Bo's phone plays Bo's own turn; a move for another game is refused.
        service.play(id, phone.token(), GameType.DARES, DareGame.class, (game, actor) -> {
            actor.requireFor(2);
            game.draw(2, DareGame.Level.WILD);
        });
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.play(id, host.token(), GameType.GUILTY, GuiltyGame.class,
                (game, actor) -> game.nextRound())));
    }

    @Test
    void sipsWonAreHandedOutToSomeoneWhoDrinksThem() {
        RoomService.Joined host = service.create(Room.Mode.QUICKPLAY, "dares", List.of("Anna", "Bo", "Cy"));
        String id = host.roomId();
        service.start(id, host.token());
        RoomView view = service.play(id, host.token(), GameType.DARES, DareGame.class, (game, actor) -> {
            game.draw(1, DareGame.Level.WILD);
            game.resolve(1, true);
        });
        assertEquals(3, view.players().get(0).pendingGive());
        assertEquals(0, view.players().get(0).sipsGiven());

        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.handOut(id, host.token(), 1, 1, 1)));
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.handOut(id, host.token(), 1, 2, 4)));
        service.handOut(id, host.token(), 1, 2, 2);
        view = service.handOut(id, host.token(), 1, 3, 1);
        assertEquals(0, view.players().get(0).pendingGive());
        assertEquals(3, view.players().get(0).sipsGiven());
        assertEquals(2, view.players().get(1).sipsDrunk());
        assertEquals(1, view.players().get(2).sipsDrunk());
        int drunk = view.players().stream().mapToInt(RoomView.PlayerView::sipsDrunk).sum();
        int given = view.players().stream().mapToInt(RoomView.PlayerView::sipsGiven).sum();
        assertEquals(given, drunk, "every sip given is a sip someone drank");
    }

    @Test
    void aFullNightSkipsGamesTheGroupIsTooSmallFor() {
        RoomService.Joined host = service.create(Room.Mode.FULL, null, List.of("Anna", "Bo"));
        String id = host.roomId();
        RoomView view = service.start(id, host.token());
        assertEquals("blackjack", view.currentGame().id());
        assertEquals("Dare or Drink", view.nextGame());
        view = service.next(id, host.token());
        view = service.next(id, host.token());
        view = service.next(id, host.token());
        assertEquals("ring-of-fire", view.currentGame().id());
        assertEquals("Derby", view.nextGame(), "Guilty! needs three players");
        view = service.next(id, host.token());
        assertEquals("derby", view.currentGame().id());
        assertEquals(Slot.Status.SKIPPED, view.lineup().get(4).status());
        assertNull(view.nextGame());
        view = service.next(id, host.token());
        assertEquals(Room.Phase.FINISHED, view.phase());
        assertNull(view.game());

        RoomService.Joined quick = service.create(Room.Mode.QUICKPLAY, "guilty", List.of("Anna", "Bo"));
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.start(quick.roomId(), quick.token())));
    }

    @Test
    void aPhoneCanTakeOverAnOpenSeatMidGameAndTheHostCanTakeItBack() {
        RoomService.Joined host = service.create(Room.Mode.QUICKPLAY, "dares", List.of("Anna", "Bo"));
        String id = host.roomId();
        String code = host.room().code();
        service.start(id, host.token());

        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.join(code, "Late", null)), "no new seats mid-game");
        RoomService.Joined phone = service.join(code, null, 2);
        assertEquals(2, phone.room().you().playerId());
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.join(code, null, 2)), "the seat is taken now");

        service.removeOrRelease(id, host.token(), 2);
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> service.view(id, phone.token())));
        assertFalse(service.view(id, host.token()).players().get(1).remote());
        assertEquals(2, service.view(id, host.token()).players().size(), "the seat itself stays in the game");
    }

    @Test
    void rematchKeepsTheCrewAndTheirPhonesButClearsTheScores() {
        RoomService.Joined host = service.create(Room.Mode.QUICKPLAY, "dares", List.of("Anna"));
        RoomService.Joined phone = service.join(host.room().code(), "Bo", null);
        String id = host.roomId();
        service.start(id, host.token());
        service.play(id, host.token(), GameType.DARES, DareGame.class, (game, actor) -> {
            game.draw(1, DareGame.Level.MILD);
            game.resolve(1, false);
        });
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> service.rematch(id, host.token())));
        assertEquals(Room.Phase.FINISHED, service.next(id, host.token()).phase());

        RoomView lobby = service.rematch(id, host.token());
        assertEquals(Room.Phase.LOBBY, lobby.phase());
        assertEquals(0, lobby.players().get(0).sipsDrunk());
        assertEquals("PLAYER", service.view(id, phone.token()).you().role(), "Bo's phone is still seated");
        assertEquals(Room.Phase.PLAYING, service.start(id, host.token()).phase());
    }

    @Test
    void idleRoomsExpireAndTheServerHasACapacity() {
        RoomService small = newService("", 1);
        RoomService.Joined first = small.create(Room.Mode.FULL, null, List.of("Anna"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, statusOf(() -> small.create(Room.Mode.FULL, null, List.of("Bo"))));

        clock.advance(Duration.ofHours(5));
        small.view(first.roomId(), first.token()); // activity keeps it alive
        clock.advance(Duration.ofHours(5));
        small.removeIdleRooms();
        assertNotNull(small.view(first.roomId(), first.token()));
        clock.advance(Duration.ofHours(7));
        small.removeIdleRooms();
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> small.view(first.roomId(), first.token())));

        RoomService.Joined second = small.create(Room.Mode.FULL, null, List.of("Bo"));
        small.end(second.roomId(), second.token());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> small.view(second.roomId(), second.token())));
    }

    @Test
    void aNightSurvivesAServerRestart(@TempDir Path dir) {
        RoomService before = newService(dir.toString(), 100);
        RoomService.Joined host = before.create(Room.Mode.FULL, null, List.of("Anna", "Bo", "Cy"));
        RoomService.Joined phone = before.join(host.room().code(), "Dee", null);
        String id = host.roomId();
        before.start(id, host.token());
        before.next(id, host.token()); // on to Dare or Drink
        before.play(id, host.token(), GameType.DARES, DareGame.class, (game, actor) -> {
            game.draw(1, DareGame.Level.WILD);
            game.resolve(1, true);
            game.draw(2, DareGame.Level.MILD);
        });
        before.saveChangedRooms();

        RoomService after = newService(dir.toString(), 100);
        after.restore();
        RoomView restored = after.view(id, host.token());
        assertEquals("dares", restored.currentGame().id());
        assertEquals(3, restored.players().get(0).pendingGive());
        assertEquals("PLAYER", after.view(id, phone.token()).you().role(), "phone tokens survive too");
        assertEquals(host.room().code(), after.lookup(host.room().code()).code());

        // The restored game is still wired to the restored players: Bo chickens out and the sip bank sees it.
        RoomView played = after.play(id, host.token(), GameType.DARES, DareGame.class,
                (game, actor) -> game.resolve(2, false));
        assertEquals(2, played.players().get(1).sipsDrunk());

        after.end(id, host.token());
        RoomService third = newService(dir.toString(), 100);
        third.restore();
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> third.view(id, host.token())), "ended nights are not restored");
    }
}
