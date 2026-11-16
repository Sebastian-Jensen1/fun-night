package com.funnight.games.higherlower;

import com.funnight.cards.Card;
import com.funnight.cards.CardView;
import com.funnight.cards.Deck;
import com.funnight.common.ApiException;
import com.funnight.game.Game;
import com.funnight.game.Progress;
import com.funnight.room.Actor;
import com.funnight.room.Player;
import com.funnight.room.Room;

import java.io.Serializable;

/**
 * Higher or Lower: guess whether the next card beats the one showing. Every right guess grows
 * the pile; a wrong guess (or the same rank) drinks one sip per card in the pile, capped, and
 * the turn moves on. Three right in a row and you may pass the pile to the next player.
 */
public class HigherLowerGame implements Game {
    public static final int STREAK_TO_PASS = 3;
    public static final int MAX_SIPS = 5;

    public enum Guess { HIGHER, LOWER }

    public record LastResult(int playerId, String playerName, Guess guess, Card from, Card drawn,
                             boolean correct, boolean passed, int sipsDrunk) implements Serializable {
    }

    private final Room room;
    private final Deck deck;
    private Card current;
    private int currentPlayer;
    private int streak;
    private int pile = 1;
    private int turnsFinished;
    private LastResult lastResult;

    public HigherLowerGame(Room room, Deck deck) {
        this.room = room;
        this.deck = deck;
        this.current = deck.draw();
    }

    public void guess(int playerId, Guess guess) {
        Player player = requireTurn(playerId);
        Card drawn = deck.draw();
        int diff = drawn.rank().strength() - current.rank().strength();
        boolean correct = guess == Guess.HIGHER ? diff > 0 : diff < 0;
        int sips = 0;
        if (correct) {
            streak++;
            pile++;
        } else {
            sips = sipsAtStake();
            player.drink(sips);
            pile = 1;
            nextPlayer();
        }
        lastResult = new LastResult(player.getId(), player.getName(), guess, current, drawn, correct, false, sips);
        current = drawn;
    }

    /** Hands the pile to the next player. Only allowed after enough right guesses in a row. */
    public void pass(int playerId) {
        Player player = requireTurn(playerId);
        if (!canPass()) throw ApiException.rule("Get " + STREAK_TO_PASS + " right in a row before you can pass.");
        lastResult = new LastResult(player.getId(), player.getName(), null, current, null, true, true, 0);
        nextPlayer();
    }

    private Player requireTurn(int playerId) {
        Player player = getCurrentPlayer();
        if (player.getId() != playerId) throw ApiException.rule("It's " + player.getName() + "'s turn.");
        return player;
    }

    private void nextPlayer() {
        streak = 0;
        turnsFinished++;
        currentPlayer = (currentPlayer + 1) % room.getPlayers().size();
    }

    public Card getCurrent() { return current; }
    public Player getCurrentPlayer() { return room.getPlayers().get(currentPlayer); }
    public int getStreak() { return streak; }
    public int getPile() { return pile; }
    public int sipsAtStake() { return Math.min(pile, MAX_SIPS); }
    public boolean canPass() { return streak >= STREAK_TO_PASS; }
    public LastResult getLastResult() { return lastResult; }

    @Override
    public Progress progress() {
        return new Progress(turnsFinished, Math.max(4, room.getPlayers().size() * 2), "turns");
    }

    public record ResultView(int playerId, String playerName, Guess guess, CardView from, CardView drawn,
                             boolean correct, boolean passed, int sipsDrunk) {
    }

    public record View(CardView currentCard, int currentPlayerId, String currentPlayerName, int streak, int pile,
                       int sipsAtStake, boolean canPass, int streakToPass, int maxSips, ResultView lastResult) {
    }

    @Override
    public View view(Actor viewer) {
        LastResult r = lastResult;
        ResultView result = r == null ? null : new ResultView(r.playerId(), r.playerName(), r.guess(),
                CardView.of(r.from()), CardView.of(r.drawn()), r.correct(), r.passed(), r.sipsDrunk());
        return new View(CardView.of(current), getCurrentPlayer().getId(), getCurrentPlayer().getName(), streak, pile,
                sipsAtStake(), canPass(), STREAK_TO_PASS, MAX_SIPS, result);
    }
}
