package com.funnight.cards;

/** A face-up card as sent to the browser. */
public record CardView(String rank, Suit suit) {
    public static CardView of(Card c) {
        return c == null ? null : new CardView(c.rank().getLabel(), c.suit());
    }
}
