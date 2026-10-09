package com.funnight.backend.dares;

import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.model.Challenge;
import com.funnight.backend.session.Player;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Players take turns drawing a dare. Do it and you hand out a sip; chicken out and drink.
 * Each session has its own deck so dares don't repeat until everything has been drawn.
 * Not thread-safe; callers hold the session lock.
 */
public class DareGame {
    public static final int CHICKEN_SIPS = 3;
    public static final int COMPLETED_SIPS_TO_GIVE = 1;

    public enum Phase { DRAW, RESOLVE }

    public record LastResult(int playerId, String playerName, String dare, boolean completed, int sipsDrunk, int sipsGiven) {
    }

    private final List<Challenge> allDares;
    private final List<Player> players;
    private final Deque<Challenge> deck = new ArrayDeque<>();
    private final SecureRandom random = new SecureRandom();
    private int currentPlayer;
    private int turn = 1;
    private Phase phase = Phase.DRAW;
    private String currentDare;
    private LastResult lastResult;

    public DareGame(List<Challenge> allDares, List<Player> players) {
        if (allDares.isEmpty()) throw new GameRuleException("There are no dares to draw.");
        this.allDares = List.copyOf(allDares);
        this.players = players;
    }

    public void draw(int playerId) {
        requireTurn(playerId, Phase.DRAW);
        if (deck.isEmpty()) {
            List<Challenge> shuffled = new ArrayList<>(allDares);
            Collections.shuffle(shuffled, random);
            deck.addAll(shuffled);
        }
        currentDare = deck.pop().getDescription();
        phase = Phase.RESOLVE;
    }

    public void resolve(int playerId, boolean completed) {
        requireTurn(playerId, Phase.RESOLVE);
        Player player = players.get(currentPlayer);
        int drunk = completed ? 0 : CHICKEN_SIPS;
        int given = completed ? COMPLETED_SIPS_TO_GIVE : 0;
        player.drink(drunk);
        player.give(given);
        lastResult = new LastResult(player.getId(), player.getName(), currentDare, completed, drunk, given);
        currentDare = null;
        phase = Phase.DRAW;
        currentPlayer = (currentPlayer + 1) % players.size();
        turn++;
    }

    private void requireTurn(int playerId, Phase expected) {
        Player player = players.get(currentPlayer);
        if (player.getId() != playerId) {
            throw new GameRuleException("It's " + player.getName() + "'s turn.");
        }
        if (phase != expected) {
            throw new GameRuleException(expected == Phase.DRAW ? "Finish your current dare first." : "Draw a dare first.");
        }
    }

    public Phase getPhase() { return phase; }
    public int getTurn() { return turn; }
    public String getCurrentDare() { return currentDare; }
    public LastResult getLastResult() { return lastResult; }
    public Player getCurrentPlayer() { return players.get(currentPlayer); }
}
