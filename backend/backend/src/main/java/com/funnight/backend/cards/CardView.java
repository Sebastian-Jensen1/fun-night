package com.funnight.backend.cards;

/** A face-up card as sent to the browser. */
public record CardView(String rank, Suit suit) {
    public static CardView of(Card c) {
        return new CardView(c.rank().getLabel(), c.suit());
    }
}
