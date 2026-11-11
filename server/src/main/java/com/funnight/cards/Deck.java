package com.funnight.cards;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One or more 52-card decks shuffled together. Reshuffles itself when it runs out. */
public class Deck implements Serializable {
    private final int decks;
    private final ArrayDeque<Card> cards = new ArrayDeque<>();
    private boolean stacked;

    private Deck(int decks) {
        this.decks = decks;
    }

    public static Deck shuffled(int decks) {
        Deck deck = new Deck(decks);
        deck.shuffle();
        return deck;
    }

    /** A deck that deals exactly these cards in order, for tests. Falls back to a fresh shuffle when empty. */
    public static Deck stacked(List<Card> order) {
        Deck deck = new Deck(1);
        deck.cards.addAll(order);
        deck.stacked = true;
        return deck;
    }

    /** All 52 cards in a fixed order. */
    public static List<Card> fullDeck() {
        List<Card> all = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                all.add(new Card(rank, suit));
            }
        }
        return all;
    }

    public void shuffle() {
        List<Card> fresh = new ArrayList<>(decks * 52);
        for (int d = 0; d < decks; d++) {
            fresh.addAll(fullDeck());
        }
        Collections.shuffle(fresh, Rng.SECURE);
        cards.clear();
        cards.addAll(fresh);
        stacked = false;
    }

    public Card draw() {
        if (cards.isEmpty()) shuffle();
        return cards.pop();
    }

    public int remaining() {
        return cards.size();
    }

    /** True once three quarters of the cards have been played. */
    public boolean runningLow() {
        return !stacked && cards.size() < decks * 52 / 4;
    }
}
