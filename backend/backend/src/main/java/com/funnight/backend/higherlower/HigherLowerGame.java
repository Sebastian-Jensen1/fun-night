package com.funnight.backend.higherlower;

import com.funnight.backend.cards.Card;
import com.funnight.backend.cards.Shoe;
import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.session.Player;

import java.util.List;

/**
 * Higher or Lower: guess whether the next card beats the one showing. Every right guess grows
 * the pile; a wrong guess (or the same rank) drinks one sip per card in the pile, capped, and
 * the turn moves on. Three right in a row and you may pass the pile to the next player.
 * Not thread-safe; callers hold the session lock.
 */
public class HigherLowerGame {
    public static final int STREAK_TO_PASS = 3;
    public static final int MAX_SIPS = 5;

    public enum Guess { HIGHER, LOWER }

    public record LastResult(int playerId, String playerName, Guess guess, Card from, Card drawn,
                             boolean correct, boolean passed, int sipsDrunk) {
    }

    private final Shoe shoe;
    private final List<Player> players;
    private Card current;
    private int currentPlayer;
    private int streak;
    private int pile = 1;
    private LastResult lastResult;

    public HigherLowerGame(Shoe shoe, List<Player> players) {
        this.shoe = shoe;
        this.players = players;
        this.current = shoe.draw();
    }

    public void guess(int playerId, Guess guess) {
        Player player = requireTurn(playerId);
        Card drawn = shoe.draw();
        int diff = drawn.rank().strength() - current.rank().strength();
        boolean correct = guess == Guess.HIGHER ? diff > 0 : diff < 0;
        int sips = 0;
        if (correct) {
            streak++;
            pile++;
        } else {
            sips = Math.min(pile, MAX_SIPS);
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
        if (streak < STREAK_TO_PASS) {
            throw new GameRuleException("Get " + STREAK_TO_PASS + " right in a row before you can pass.");
        }
        lastResult = new LastResult(player.getId(), player.getName(), null, current, null, true, true, 0);
        nextPlayer();
    }

    private Player requireTurn(int playerId) {
        Player player = players.get(currentPlayer);
        if (player.getId() != playerId) {
            throw new GameRuleException("It's " + player.getName() + "'s turn.");
        }
        return player;
    }

    private void nextPlayer() {
        streak = 0;
        currentPlayer = (currentPlayer + 1) % players.size();
    }

    public Card getCurrent() { return current; }
    public Player getCurrentPlayer() { return players.get(currentPlayer); }
    public int getStreak() { return streak; }
    public int getPile() { return pile; }
    public int sipsAtStake() { return Math.min(pile, MAX_SIPS); }
    public boolean canPass() { return streak >= STREAK_TO_PASS; }
    public LastResult getLastResult() { return lastResult; }
    public int cardsLeft() { return shoe.remaining(); }
}
