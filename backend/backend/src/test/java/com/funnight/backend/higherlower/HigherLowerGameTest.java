package com.funnight.backend.higherlower;

import com.funnight.backend.cards.Card;
import com.funnight.backend.cards.Rank;
import com.funnight.backend.cards.Shoe;
import com.funnight.backend.cards.Suit;
import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.session.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.funnight.backend.higherlower.HigherLowerGame.Guess.HIGHER;
import static com.funnight.backend.higherlower.HigherLowerGame.Guess.LOWER;
import static org.junit.jupiter.api.Assertions.*;

class HigherLowerGameTest {
    private Player a;
    private Player b;

    @BeforeEach
    void setUp() {
        a = new Player(1, "A");
        b = new Player(2, "B");
    }

    private HigherLowerGame game(Rank... order) {
        List<Card> cards = Arrays.stream(order).map(r -> new Card(r, Suit.CLUBS)).toList();
        return new HigherLowerGame(Shoe.stacked(cards), List.of(a, b));
    }

    @Test
    void rightGuessesGrowThePileAndWrongOneDrinksIt() {
        HigherLowerGame g = game(Rank.FIVE, Rank.NINE, Rank.TWO, Rank.KING);
        g.guess(1, HIGHER);          // 5 -> 9
        g.guess(1, LOWER);           // 9 -> 2
        assertEquals(3, g.getPile());
        assertEquals(2, g.getStreak());

        g.guess(1, LOWER);           // 2 -> K, wrong
        assertEquals(3, a.getSipsDrunk());
        assertEquals(1, g.getPile());
        assertEquals(2, g.getCurrentPlayer().getId());
        assertFalse(g.getLastResult().correct());
    }

    @Test
    void sameRankCountsAsWrong() {
        HigherLowerGame g = game(Rank.SEVEN, Rank.SEVEN);
        g.guess(1, HIGHER);
        assertEquals(1, a.getSipsDrunk());
    }

    @Test
    void sipsAreCapped() {
        HigherLowerGame g = game(Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.TWO);
        for (int i = 0; i < 6; i++) g.guess(1, HIGHER);
        assertEquals(7, g.getPile());
        g.guess(1, HIGHER);
        assertEquals(HigherLowerGame.MAX_SIPS, a.getSipsDrunk());
    }

    @Test
    void passingNeedsAStreakAndHandsThePileOn() {
        HigherLowerGame g = game(Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.ACE);
        assertThrows(GameRuleException.class, () -> g.pass(1));
        g.guess(1, HIGHER);
        g.guess(1, HIGHER);
        assertThrows(GameRuleException.class, () -> g.pass(1));
        g.guess(1, HIGHER);
        g.pass(1);

        assertEquals(2, g.getCurrentPlayer().getId());
        assertEquals(4, g.getPile(), "the pile goes with the turn");
        assertEquals(0, g.getStreak());
        g.guess(2, LOWER);           // 5 -> A, wrong
        assertEquals(4, b.getSipsDrunk());
    }

    @Test
    void onlyTheCurrentPlayerCanGuess() {
        HigherLowerGame g = game(Rank.FIVE, Rank.NINE);
        assertThrows(GameRuleException.class, () -> g.guess(2, HIGHER));
    }
}
