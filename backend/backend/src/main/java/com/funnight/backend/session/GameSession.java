package com.funnight.backend.session;

import com.funnight.backend.blackjack.BlackjackGame;
import com.funnight.backend.dares.DareGame;
import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.game.GameType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A group of players sitting down for a night of games. All state changes must happen while
 * holding this object's monitor; {@link SessionService#withSession} takes care of that.
 */
public class GameSession {
    private final String id;
    private final byte[] tokenHash;
    private final SessionMode mode;
    private final List<Player> players;
    private final List<GameSlot> slots;
    private final Instant createdAt;
    private volatile Instant lastActivity;
    private int currentIndex = -1;
    private boolean finished;

    private BlackjackGame blackjack;
    private DareGame dares;

    GameSession(String id, byte[] tokenHash, SessionMode mode, List<Player> players, List<GameType> lineUp, Instant now) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.mode = mode;
        this.players = List.copyOf(players);
        this.slots = lineUp.stream().map(GameSlot::new).toList();
        this.createdAt = now;
        this.lastActivity = now;
        moveToNextPlayableGame();
    }

    public String getId() { return id; }
    byte[] getTokenHash() { return tokenHash; }
    public SessionMode getMode() { return mode; }
    public List<Player> getPlayers() { return players; }
    public List<GameSlot> getSlots() { return slots; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActivity() { return lastActivity; }
    public int getCurrentIndex() { return currentIndex; }
    public boolean isFinished() { return finished; }

    void touch(Instant now) { lastActivity = now; }

    public Optional<GameType> currentGame() {
        return finished ? Optional.empty() : Optional.of(slots.get(currentIndex).getType());
    }

    public Optional<Player> findPlayer(int playerId) {
        return players.stream().filter(p -> p.getId() == playerId).findFirst();
    }

    /** Guards game endpoints so e.g. blackjack can't be played while the session is on another game. */
    public void requireCurrentGame(GameType type) {
        if (currentGame().filter(type::equals).isEmpty()) {
            throw new GameRuleException(type.getDisplayName() + " is not the game being played right now.");
        }
    }

    public BlackjackGame blackjack(Supplier<BlackjackGame> factory) {
        requireCurrentGame(GameType.BLACKJACK);
        if (blackjack == null) blackjack = factory.get();
        return blackjack;
    }

    public DareGame dares(Supplier<DareGame> factory) {
        requireCurrentGame(GameType.DARES);
        if (dares == null) dares = factory.get();
        return dares;
    }

    /** Completes the current game and moves on, skipping games that aren't playable yet. */
    void advance() {
        if (finished) throw new GameRuleException("This session is already finished.");
        slots.get(currentIndex).setStatus(GameSlot.Status.COMPLETED);
        moveToNextPlayableGame();
    }

    private void moveToNextPlayableGame() {
        for (int i = currentIndex + 1; i < slots.size(); i++) {
            GameSlot slot = slots.get(i);
            if (slot.getType().isAvailable()) {
                slot.setStatus(GameSlot.Status.ACTIVE);
                currentIndex = i;
                return;
            }
            slot.setStatus(GameSlot.Status.SKIPPED);
        }
        finished = true;
    }
}
