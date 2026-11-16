package com.funnight.room;

import com.funnight.common.ApiException;
import com.funnight.game.Game;
import com.funnight.game.GameFactory;
import com.funnight.game.GameType;

import java.io.Serializable;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A group of friends and their night of games: a lobby where people take a seat, then the
 * line-up of games, then the final scores. All state changes must happen while holding this
 * object's monitor; {@link RoomService} takes care of that.
 */
public class Room implements Serializable {
    public enum Mode { FULL, QUICKPLAY }
    public enum Phase { LOBBY, PLAYING, FINISHED }

    public static final int MAX_PLAYERS = 8;

    private final String id;
    private final String code;
    private final Mode mode;
    private final byte[] hostTokenHash;
    private final List<Player> players = new ArrayList<>();
    private final List<Slot> slots = new ArrayList<>();
    private final Instant createdAt;
    private volatile Instant lastActivity;
    private Phase phase = Phase.LOBBY;
    private int currentIndex = -1;
    private int nextPlayerId = 1;
    private long version = 1;
    private Game game;

    public Room(String id, String code, Mode mode, GameType quickGame, byte[] hostTokenHash, Instant now) {
        this.id = id;
        this.code = code;
        this.mode = mode;
        this.hostTokenHash = hostTokenHash;
        this.createdAt = now;
        this.lastActivity = now;
        List<GameType> lineUp = mode == Mode.FULL ? Arrays.asList(GameType.values()) : List.of(quickGame);
        lineUp.forEach(type -> slots.add(new Slot(type)));
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public Mode getMode() { return mode; }
    public Phase getPhase() { return phase; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public List<Slot> getSlots() { return Collections.unmodifiableList(slots); }
    public int getCurrentIndex() { return currentIndex; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActivity() { return lastActivity; }
    public long getVersion() { return version; }
    public Game getGame() { return game; }

    void touch(Instant now) { lastActivity = now; }
    void bumpVersion() { version++; }

    /** Works out who a token belongs to, or returns null for a token this room doesn't know. */
    Actor identify(byte[] tokenHash) {
        if (MessageDigest.isEqual(hostTokenHash, tokenHash)) return Actor.ofHost();
        for (Player p : players) {
            if (p.isRemote() && MessageDigest.isEqual(p.getTokenHash(), tokenHash)) return Actor.ofPlayer(p.getId());
        }
        return null;
    }

    public Player player(int playerId) {
        return players.stream().filter(p -> p.getId() == playerId).findFirst()
                .orElseThrow(() -> ApiException.badRequest("That player isn't in this game."));
    }

    public boolean canPlay(GameType type) {
        return players.size() >= type.getMinPlayers();
    }

    public Optional<GameType> currentGame() {
        return phase == Phase.PLAYING ? Optional.of(slots.get(currentIndex).getType()) : Optional.empty();
    }

    /** The next game this group can actually play after the current one, if any. */
    public Optional<GameType> nextGame() {
        return slots.stream().skip(currentIndex + 1).map(Slot::getType).filter(this::canPlay).findFirst();
    }

    // ---- lobby -------------------------------------------------------------------------------

    /** Adds a seat. {@code tokenHash} is null for a seat played on the host device. */
    public Player addPlayer(String name, byte[] tokenHash) {
        if (phase != Phase.LOBBY) throw ApiException.rule("The night has already started.");
        if (players.size() >= MAX_PLAYERS) throw ApiException.rule("The table is full (max " + MAX_PLAYERS + " players).");
        if (players.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name))) {
            throw ApiException.badRequest("Two players can't both be called \"" + name + "\".");
        }
        Player player = new Player(nextPlayerId++, name, tokenHash);
        players.add(player);
        return player;
    }

    void removePlayer(int playerId) {
        if (phase != Phase.LOBBY) throw ApiException.rule("Players can't leave the table mid-game.");
        players.remove(player(playerId));
    }

    void start(GameFactory factory) {
        if (phase != Phase.LOBBY) throw ApiException.rule("The night has already started.");
        if (players.isEmpty()) throw ApiException.rule("Add at least one player first.");
        slots.forEach(slot -> slot.setStatus(Slot.Status.UPCOMING));
        GameType first = slots.get(0).getType();
        if (mode == Mode.QUICKPLAY && !canPlay(first)) {
            throw ApiException.rule(first.getDisplayName() + " needs at least " + first.getMinPlayers() + " players.");
        }
        phase = Phase.PLAYING;
        currentIndex = -1;
        moveToNextPlayableGame(factory);
    }

    // ---- playing -----------------------------------------------------------------------------

    /**
     * Returns the state of {@code type}, failing if the room is currently on a different game
     * so one game's endpoints can't be played during another.
     */
    public <T extends Game> T game(GameType type, Class<T> stateClass) {
        if (currentGame().filter(type::equals).isEmpty()) {
            throw ApiException.rule(type.getDisplayName() + " is not the game being played right now.");
        }
        return stateClass.cast(game);
    }

    /** Completes the current game and moves on, skipping games this group is too small for. */
    void advance(GameFactory factory) {
        if (phase != Phase.PLAYING) throw ApiException.rule("There is no game in progress.");
        slots.get(currentIndex).setStatus(Slot.Status.COMPLETED);
        moveToNextPlayableGame(factory);
    }

    private void moveToNextPlayableGame(GameFactory factory) {
        game = null;
        for (int i = currentIndex + 1; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (canPlay(slot.getType())) {
                slot.setStatus(Slot.Status.ACTIVE);
                currentIndex = i;
                game = factory.create(slot.getType(), this);
                return;
            }
            slot.setStatus(Slot.Status.SKIPPED);
        }
        phase = Phase.FINISHED;
    }

    /** Back to the lobby with the same crew (phones stay connected) and a clean sip bank. */
    void rematch() {
        if (phase != Phase.FINISHED) throw ApiException.rule("Finish the night before starting a rematch.");
        players.forEach(Player::resetScore);
        slots.forEach(slot -> slot.setStatus(Slot.Status.UPCOMING));
        currentIndex = -1;
        game = null;
        phase = Phase.LOBBY;
    }

    // ---- the sip bank ------------------------------------------------------------------------

    /**
     * A player wins sips to hand out. They stay pending until the winner picks who drinks them,
     * so every sip given is also a sip somebody drank. Playing solo there is nobody to hand
     * them to, so they count as given straight away.
     */
    public void award(Player winner, int sips) {
        if (sips <= 0) return;
        if (players.size() == 1) winner.addGiven(sips);
        else winner.addPending(sips);
    }

    /** {@code giver} makes {@code target} drink right now (no pending step). */
    public void give(Player giver, Player target, int sips) {
        target.drink(sips);
        if (giver != target) giver.addGiven(sips);
    }

    void handOut(int giverId, int targetId, int sips) {
        if (phase == Phase.LOBBY) throw ApiException.rule("There are no sips to hand out yet.");
        Player giver = player(giverId);
        Player target = player(targetId);
        if (giver == target) throw ApiException.badRequest("You can't hand sips to yourself.");
        if (sips < 1 || sips > giver.getPendingGive()) {
            throw ApiException.rule(giver.getName() + " has " + giver.getPendingGive() + " sips left to hand out.");
        }
        giver.usePending(sips);
        give(giver, target, sips);
    }
}
