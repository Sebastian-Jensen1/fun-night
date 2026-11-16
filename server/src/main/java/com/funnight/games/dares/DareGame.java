package com.funnight.games.dares;

import com.funnight.cards.Rng;
import com.funnight.common.ApiException;
import com.funnight.game.Game;
import com.funnight.game.Progress;
import com.funnight.room.Actor;
import com.funnight.room.Player;
import com.funnight.room.Room;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dare or Drink: on your turn pick a mild or a wild dare. Do it and you win sips to hand out;
 * chicken out and you drink. Wild dares pay more and cost more. Each deck is dealt all the way
 * through before any dare repeats.
 */
public class DareGame implements Game {

    public enum Level {
        MILD(1, 2), WILD(3, 4);

        private final int sipsToGive;
        private final int sipsToDrink;

        Level(int sipsToGive, int sipsToDrink) {
            this.sipsToGive = sipsToGive;
            this.sipsToDrink = sipsToDrink;
        }

        public int getSipsToGive() { return sipsToGive; }
        public int getSipsToDrink() { return sipsToDrink; }
    }

    public enum Phase { CHOOSE, RESOLVE }

    public record LastResult(int playerId, String playerName, Level level, String dare, boolean completed,
                             int sipsDrunk, int sipsToGive) implements Serializable {
    }

    private final Room room;
    private final ArrayList<String> mildDares;
    private final ArrayList<String> wildDares;
    private final ArrayDeque<String> mildDeck = new ArrayDeque<>();
    private final ArrayDeque<String> wildDeck = new ArrayDeque<>();
    private int currentPlayer;
    private int turn = 1;
    private Phase phase = Phase.CHOOSE;
    private Level level;
    private String dare;
    private LastResult lastResult;

    public DareGame(Room room, List<String> mildDares, List<String> wildDares) {
        if (mildDares.isEmpty() || wildDares.isEmpty()) throw new IllegalArgumentException("There are no dares to draw.");
        this.room = room;
        this.mildDares = new ArrayList<>(mildDares);
        this.wildDares = new ArrayList<>(wildDares);
    }

    /** The current player picks how brave they feel and draws a dare from that deck. */
    public void draw(int playerId, Level chosen) {
        requireTurn(playerId, Phase.CHOOSE);
        ArrayDeque<String> deck = chosen == Level.MILD ? mildDeck : wildDeck;
        if (deck.isEmpty()) {
            List<String> shuffled = new ArrayList<>(chosen == Level.MILD ? mildDares : wildDares);
            Collections.shuffle(shuffled, Rng.SECURE);
            deck.addAll(shuffled);
        }
        level = chosen;
        dare = deck.pop();
        phase = Phase.RESOLVE;
    }

    public void resolve(int playerId, boolean completed) {
        Player player = requireTurn(playerId, Phase.RESOLVE);
        int drunk = completed ? 0 : level.getSipsToDrink();
        int toGive = completed ? level.getSipsToGive() : 0;
        player.drink(drunk);
        room.award(player, toGive);
        lastResult = new LastResult(player.getId(), player.getName(), level, dare, completed, drunk, toGive);
        dare = null;
        level = null;
        phase = Phase.CHOOSE;
        currentPlayer = (currentPlayer + 1) % room.getPlayers().size();
        turn++;
    }

    private Player requireTurn(int playerId, Phase expected) {
        Player player = getCurrentPlayer();
        if (player.getId() != playerId) throw ApiException.rule("It's " + player.getName() + "'s turn.");
        if (phase != expected) {
            throw ApiException.rule(expected == Phase.CHOOSE ? "Finish your current dare first." : "Draw a dare first.");
        }
        return player;
    }

    public Phase getPhase() { return phase; }
    public int getTurn() { return turn; }
    public String getDare() { return dare; }
    public Level getLevel() { return level; }
    public LastResult getLastResult() { return lastResult; }
    public Player getCurrentPlayer() { return room.getPlayers().get(currentPlayer); }

    @Override
    public Progress progress() {
        return new Progress(turn - 1, room.getPlayers().size() * 2, "dares");
    }

    public record LevelView(Level id, int sipsToGive, int sipsToDrink) {
    }

    public record View(Phase phase, int turn, int currentPlayerId, String currentPlayerName, Level level, String dare,
                       List<LevelView> levels, LastResult lastResult) {
    }

    @Override
    public View view(Actor viewer) {
        List<LevelView> levels = Arrays.stream(Level.values())
                .map(l -> new LevelView(l, l.getSipsToGive(), l.getSipsToDrink())).toList();
        return new View(phase, turn, getCurrentPlayer().getId(), getCurrentPlayer().getName(), level, dare, levels, lastResult);
    }
}
