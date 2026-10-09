package com.funnight.backend.blackjack;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/** A multi-deck shoe, shuffled with a cryptographically secure RNG so the deal can't be predicted. */
public class Shoe {
    public static final int DEFAULT_DECKS = 4;

    private final int decks;
    private final Random random;
    private final Deque<Card> cards = new ArrayDeque<>();
    private boolean stacked;

    public Shoe() {
        this(DEFAULT_DECKS, new SecureRandom());
    }

    public Shoe(int decks, Random random) {
        this.decks = decks;
        this.random = random;
        shuffle();
    }

    /** A shoe that deals exactly these cards in order, for tests. Falls back to a fresh shuffle when empty. */
    public static Shoe stacked(List<Card> order) {
        Shoe shoe = new Shoe(1, new SecureRandom());
        shoe.cards.clear();
        shoe.cards.addAll(order);
        shoe.stacked = true;
        return shoe;
    }

    public void shuffle() {
        List<Card> fresh = new ArrayList<>(decks * 52);
        for (int d = 0; d < decks; d++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    fresh.add(new Card(rank, suit));
                }
            }
        }
        Collections.shuffle(fresh, random);
        cards.clear();
        cards.addAll(fresh);
    }

    public Card draw() {
        if (cards.isEmpty()) shuffle();
        return cards.pop();
    }

    public int remaining() {
        return cards.size();
    }

    /** Reshuffle once three quarters of the shoe has been played. */
    public boolean needsReshuffle() {
        return !stacked && cards.size() < decks * 52 / 4;
    }
}
