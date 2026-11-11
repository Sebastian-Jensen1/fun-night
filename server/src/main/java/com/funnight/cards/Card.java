package com.funnight.cards;

import java.io.Serializable;

public record Card(Rank rank, Suit suit) implements Serializable {
}
