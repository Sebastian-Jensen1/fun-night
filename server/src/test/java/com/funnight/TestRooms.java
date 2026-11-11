package com.funnight;

import com.funnight.cards.Card;
import com.funnight.cards.Rank;
import com.funnight.cards.Suit;
import com.funnight.room.Room;

import java.time.Instant;

/** Shared helpers for building tables and cards in tests. */
public final class TestRooms {
    private TestRooms() {
    }

    public static Room room(String... names) {
        Room room = new Room("room-1", "TEST1", Room.Mode.FULL, null, new byte[32], Instant.parse("2026-01-01T20:00:00Z"));
        for (String name : names) room.addPlayer(name, null);
        return room;
    }

    public static Card card(Rank rank) {
        return new Card(rank, Suit.SPADES);
    }

    public static Card card(Rank rank, Suit suit) {
        return new Card(rank, suit);
    }
}
