package com.funnight.backend.ringoffire;

import com.funnight.backend.cards.Card;
import com.funnight.backend.cards.Rank;
import com.funnight.backend.cards.Shoe;
import com.funnight.backend.cards.Suit;
import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.exception.InvalidRequestException;
import com.funnight.backend.session.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RingOfFireGameTest {
    private Player a;
    private Player b;
    private Player c;

    @BeforeEach
    void setUp() {
        a = new Player(1, "A");
        b = new Player(2, "B");
        c = new Player(3, "C");
    }

    private RingOfFireGame game(Rank... order) {
        List<Card> cards = Arrays.stream(order).map(r -> new Card(r, Suit.HEARTS)).toList();
        return new RingOfFireGame(() -> Shoe.stacked(cards), List.of(a, b, c));
    }

    @Test
    void waterfallMakesEveryoneDrink() {
        RingOfFireGame g = game(Rank.ACE, Rank.TWO);
        g.draw(1);
        g.resolve(1, null);
        assertEquals(List.of(1, 1, 1), List.of(a.getSipsDrunk(), b.getSipsDrunk(), c.getSipsDrunk()));
        assertEquals(2, g.getCurrentPlayer().getId());
    }

    @Test
    void youMakesSomeoneElseDrink() {
        RingOfFireGame g = game(Rank.TWO, Rank.TWO);
        g.draw(1);
        assertThrows(InvalidRequestException.class, () -> g.resolve(1, null));
        assertThrows(InvalidRequestException.class, () -> g.resolve(1, 1));
        g.resolve(1, 3);
        assertEquals(RingOfFireGame.SIPS_PER_PENALTY, c.getSipsDrunk());
        assertEquals(RingOfFireGame.SIPS_PER_PENALTY, a.getSipsGiven());
    }

    @Test
    void matesDrinkTogether() {
        RingOfFireGame g = game(Rank.EIGHT, Rank.THREE, Rank.FOUR);
        g.draw(1);
        g.resolve(1, 2);              // A and B are mates
        g.draw(2);
        g.resolve(2, null);           // B: "Me"
        assertEquals(2, b.getSipsDrunk());
        assertEquals(2, a.getSipsDrunk());
        assertEquals(List.of(List.of(a, b)), g.matePairs());
    }

    @Test
    void fourthKingDrinksTheCupAndEndsTheRing() {
        RingOfFireGame g = game(Rank.KING, Rank.KING, Rank.KING, Rank.KING, Rank.FIVE);
        for (int i = 0; i < 3; i++) {
            int id = g.getCurrentPlayer().getId();
            g.draw(id);
            g.resolve(id, null);
        }
        assertEquals(3 * RingOfFireGame.CUP_SIPS_PER_KING, g.getCupSips());

        g.draw(1);
        g.resolve(1, null);
        assertEquals(9, a.getSipsDrunk());
        assertEquals(RingOfFireGame.Phase.OVER, g.getPhase());
        assertThrows(GameRuleException.class, () -> g.draw(2));

        g.restart();
        assertEquals(RingOfFireGame.Phase.DRAW, g.getPhase());
        assertEquals(0, g.getKingsDrawn());
    }

    @Test
    void enforcesTurnsAndPhases() {
        RingOfFireGame g = game(Rank.FIVE, Rank.SIX);
        assertThrows(GameRuleException.class, () -> g.draw(2));
        assertThrows(GameRuleException.class, () -> g.resolve(1, null));
        g.draw(1);
        assertThrows(GameRuleException.class, () -> g.draw(1));
        assertThrows(InvalidRequestException.class, () -> g.resolve(1, 2));
        assertThrows(GameRuleException.class, () -> g.restart());
    }

    @Test
    void cardIsOnlyShownWhileResolving() {
        RingOfFireGame g = game(Rank.QUEEN, Rank.FIVE);
        assertNull(RingOfFireView.of(g).currentCard());
        g.draw(1);
        RingOfFireView view = RingOfFireView.of(g);
        assertEquals("Q", view.currentCard().rank());
        assertEquals("Question Master", view.rule().title());
        g.resolve(1, null);
        assertEquals("A", RingOfFireView.of(g).questionMaster());
    }
}
