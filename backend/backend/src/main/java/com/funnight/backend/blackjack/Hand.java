package com.funnight.backend.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {
    private final List<Card> cards = new ArrayList<>();

    public void add(Card card) {
        cards.add(card);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public int size() {
        return cards.size();
    }

    /** Best total, counting aces as 11 unless that would bust. */
    public int value() {
        int total = 0;
        int aces = 0;
        for (Card c : cards) {
            total += c.rank().getValue();
            if (c.rank() == Rank.ACE) aces++;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    /** True if an ace is still being counted as 11. */
    public boolean isSoft() {
        int hard = cards.stream().mapToInt(c -> c.rank() == Rank.ACE ? 1 : c.rank().getValue()).sum();
        return cards.stream().anyMatch(c -> c.rank() == Rank.ACE) && hard + 10 <= 21;
    }

    public boolean isBlackjack() {
        return cards.size() == 2 && value() == 21;
    }

    public boolean isBust() {
        return value() > 21;
    }
}
