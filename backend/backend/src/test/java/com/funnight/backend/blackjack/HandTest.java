package com.funnight.backend.blackjack;

import com.funnight.backend.cards.Card;
import com.funnight.backend.cards.Rank;
import com.funnight.backend.cards.Suit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandTest {

    private static Hand hand(Rank... ranks) {
        Hand h = new Hand();
        for (Rank r : ranks) h.add(new Card(r, Suit.SPADES));
        return h;
    }

    @Test
    void acesDropToOneWhenNeeded() {
        assertEquals(21, hand(Rank.ACE, Rank.KING).value());
        assertEquals(12, hand(Rank.ACE, Rank.ACE).value());
        assertEquals(13, hand(Rank.ACE, Rank.FIVE, Rank.SEVEN).value());
        assertEquals(21, hand(Rank.ACE, Rank.ACE, Rank.NINE).value());
    }

    @Test
    void blackjackOnlyWithTwoCards() {
        assertTrue(hand(Rank.ACE, Rank.QUEEN).isBlackjack());
        assertFalse(hand(Rank.SEVEN, Rank.SEVEN, Rank.SEVEN).isBlackjack());
    }

    @Test
    void softAndBust() {
        assertTrue(hand(Rank.ACE, Rank.SIX).isSoft());
        assertFalse(hand(Rank.ACE, Rank.SIX, Rank.TEN).isSoft());
        assertTrue(hand(Rank.KING, Rank.QUEEN, Rank.TWO).isBust());
    }
}
