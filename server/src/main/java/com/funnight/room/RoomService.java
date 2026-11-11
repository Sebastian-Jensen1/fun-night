package com.funnight.room;

import com.funnight.common.ApiException;
import com.funnight.game.Game;
import com.funnight.game.GameFactory;
import com.funnight.game.GameType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

@Service
public class RoomService {
    public static final int MAX_NAME_LENGTH = 20;
    public static final int CODE_LENGTH = 5;
    /** No 0/O or 1/I, so a code read out loud across a noisy room can't be mistyped. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} _.'!-]{1," + MAX_NAME_LENGTH + "}$");
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    public record Joined(String roomId, String token, RoomView room) {
    }

    public record Lookup(String code, Room.Mode mode, Room.Phase phase, boolean full, List<Seat> seats) {
        public record Seat(int id, String name, boolean remote) {
        }
    }

    private final RoomStore store;
    private final RoomSnapshots snapshots;
    private final RoomEvents events;
    private final GameFactory games;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Object createLock = new Object();
    private final int maxRooms;
    private final Duration idleTimeout;

    public RoomService(RoomStore store, RoomSnapshots snapshots, RoomEvents events, GameFactory games, Clock clock,
                       @Value("${funnight.rooms.max-active:5000}") int maxRooms,
                       @Value("${funnight.rooms.idle-timeout:PT6H}") Duration idleTimeout) {
        this.store = store;
        this.snapshots = snapshots;
        this.events = events;
        this.games = games;
        this.clock = clock;
        this.maxRooms = maxRooms;
        this.idleTimeout = idleTimeout;
    }

    // ---- creating and joining ----------------------------------------------------------------

    /** Opens a lobby. {@code playerNames} are seats played on the host device; phones join later. */
    public Joined create(Room.Mode mode, String gameId, List<String> playerNames) {
        GameType quickGame = null;
        if (mode == Room.Mode.QUICKPLAY) {
            quickGame = GameType.fromId(gameId == null ? "" : gameId)
                    .orElseThrow(() -> ApiException.badRequest("Pick a game for quick play."));
        }
        String token = newToken();
        Room room;
        // One at a time, so the capacity check and the unique join code can't be raced
        synchronized (createLock) {
            if (store.count() >= maxRooms) {
                throw ApiException.busy("The server is full right now. Please try again in a little while.");
            }
            room = new Room(UUID.randomUUID().toString(), newCode(), mode, quickGame, hash(token), clock.instant());
            if (playerNames != null) {
                for (String name : playerNames) room.addPlayer(cleanName(name), null);
            }
            store.save(room);
        }
        snapshots.markDirty(room.getId());
        return new Joined(room.getId(), token, RoomView.of(room, Actor.ofHost(), events));
    }

    /** What a phone sees after typing or scanning a join code, before it has a seat. */
    public Lookup lookup(String code) {
        Room room = findByCode(code);
        synchronized (room) {
            return new Lookup(room.getCode(), room.getMode(), room.getPhase(),
                    room.getPlayers().size() >= Room.MAX_PLAYERS,
                    room.getPlayers().stream().map(p -> new Lookup.Seat(p.getId(), p.getName(), p.isRemote())).toList());
        }
    }

    /**
     * A phone takes a seat: a brand-new one (lobby only, needs {@code name}) or an existing seat
     * that's currently played on the host device ({@code seatId}, also works mid-game).
     */
    public Joined join(String code, String name, Integer seatId) {
        Room room = findByCode(code);
        String token = newToken();
        RoomView view;
        synchronized (room) {
            if (room.getPhase() == Room.Phase.FINISHED) throw ApiException.rule("That game night is already over.");
            Player player;
            if (seatId != null) {
                player = room.player(seatId);
                if (player.isRemote()) throw ApiException.rule(player.getName() + "'s seat is already on another phone.");
                player.claim(hash(token));
            } else {
                if (room.getPhase() != Room.Phase.LOBBY) {
                    throw ApiException.rule("The night has already started. Pick one of the open seats instead.");
                }
                player = room.addPlayer(cleanName(name), hash(token));
            }
            room.touch(clock.instant());
            room.bumpVersion();
            view = RoomView.of(room, Actor.ofPlayer(player.getId()), events);
        }
        changed(room);
        return new Joined(room.getId(), token, view);
    }

    // ---- reading and changing ----------------------------------------------------------------

    public RoomView view(String roomId, String token) {
        Room room = find(roomId);
        synchronized (room) {
            Actor actor = authenticate(room, token);
            room.touch(clock.instant());
            return RoomView.of(room, actor, events);
        }
    }

    /**
     * Checks the caller's token and runs {@code action} while holding the room's lock, so two
     * screens tapping at once can't corrupt a game. Everyone in the room is then told to refresh.
     */
    public RoomView mutate(String roomId, String token, BiConsumer<Room, Actor> action) {
        Room room = find(roomId);
        RoomView view;
        synchronized (room) {
            Actor actor = authenticate(room, token);
            room.touch(clock.instant());
            action.accept(room, actor);
            room.bumpVersion();
            view = RoomView.of(room, actor, events);
        }
        changed(room);
        return view;
    }

    /** Runs a move in the game the room is currently on. */
    public <G extends Game> RoomView play(String roomId, String token, GameType type, Class<G> stateClass,
                                          BiConsumer<G, Actor> move) {
        return mutate(roomId, token, (room, actor) -> move.accept(room.game(type, stateClass), actor));
    }

    public SseEmitter subscribe(String roomId, String token) {
        Room room = find(roomId);
        Actor actor;
        long version;
        synchronized (room) {
            actor = authenticate(room, token);
            version = room.getVersion();
        }
        return events.subscribe(roomId, actor, version);
    }

    public RoomView addLocalPlayer(String roomId, String token, String name) {
        return mutate(roomId, token, (room, actor) -> {
            actor.requireHost();
            room.addPlayer(cleanName(name), null);
        });
    }

    /**
     * In the lobby this removes the seat. Mid-game the seat stays but is handed back to the host
     * device, e.g. when a phone ran out of battery. Allowed for the host and for the player's own phone.
     */
    public RoomView removeOrRelease(String roomId, String token, int playerId) {
        RoomView view = mutate(roomId, token, (room, actor) -> {
            actor.requireFor(playerId);
            Player player = room.player(playerId);
            if (room.getPhase() == Room.Phase.LOBBY) {
                room.removePlayer(playerId);
            } else {
                if (!player.isRemote()) throw ApiException.rule(player.getName() + " is already played on the host screen.");
                player.release();
            }
        });
        events.dropPlayer(roomId, playerId);
        return view;
    }

    public RoomView start(String roomId, String token) {
        return mutate(roomId, token, (room, actor) -> {
            actor.requireHost();
            room.start(games);
        });
    }

    public RoomView next(String roomId, String token) {
        return mutate(roomId, token, (room, actor) -> {
            actor.requireHost();
            room.advance(games);
        });
    }

    public RoomView rematch(String roomId, String token) {
        return mutate(roomId, token, (room, actor) -> {
            actor.requireHost();
            room.rematch();
        });
    }

    public RoomView handOut(String roomId, String token, int giverId, int targetId, int sips) {
        return mutate(roomId, token, (room, actor) -> {
            actor.requireFor(giverId);
            room.handOut(giverId, targetId, sips);
        });
    }

    public void end(String roomId, String token) {
        Room room = find(roomId);
        synchronized (room) {
            authenticate(room, token).requireHost();
            store.delete(roomId);
        }
        snapshots.delete(roomId);
        events.close(roomId);
    }

    // ---- housekeeping ------------------------------------------------------------------------

    @PostConstruct
    void restore() {
        Instant cutoff = clock.instant().minus(idleTimeout);
        int restored = 0;
        for (Room room : snapshots.loadAll()) {
            if (room.getLastActivity().isBefore(cutoff)) {
                snapshots.delete(room.getId());
            } else {
                store.save(room);
                restored++;
            }
        }
        if (restored > 0) log.info("Restored {} game night(s) from disk", restored);
    }

    @Scheduled(fixedDelay = 2000)
    @PreDestroy
    public void saveChangedRooms() {
        for (String id : snapshots.drainDirty()) {
            store.findById(id).ifPresent(snapshots::write);
            // The room may have ended while it was being written
            if (store.findById(id).isEmpty()) snapshots.delete(id);
        }
    }

    @Scheduled(fixedDelayString = "${funnight.rooms.cleanup-interval-ms:300000}")
    public void removeIdleRooms() {
        List<String> removed = store.removeIdleSince(clock.instant().minus(idleTimeout));
        for (String id : removed) {
            snapshots.delete(id);
            events.close(id);
        }
        if (!removed.isEmpty()) log.info("Removed {} idle game night(s)", removed.size());
    }

    // ---- helpers -----------------------------------------------------------------------------

    private void changed(Room room) {
        snapshots.markDirty(room.getId());
        events.publish(room.getId(), String.valueOf(room.getVersion()));
    }

    private Room find(String roomId) {
        return store.findById(roomId)
                .orElseThrow(() -> ApiException.notFound("This game night doesn't exist or has ended."));
    }

    private Room findByCode(String code) {
        String normalized = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        return store.findByCode(normalized)
                .orElseThrow(() -> ApiException.notFound("No game night found with that code."));
    }

    private Actor authenticate(Room room, String token) {
        if (token == null || token.isBlank() || token.length() > 128) throw ApiException.invalidToken();
        Actor actor = room.identify(hash(token));
        if (actor == null) throw ApiException.invalidToken();
        return actor;
    }

    static String cleanName(String raw) {
        String name = raw == null ? "" : raw.strip().replaceAll("\\s+", " ");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw ApiException.badRequest("Names may only use letters, numbers, spaces and _ . ' ! - (max "
                    + MAX_NAME_LENGTH + " characters).");
        }
        return name;
    }

    private String newCode() {
        while (true) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
            }
            if (store.findByCode(code.toString()).isEmpty()) return code.toString();
        }
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
