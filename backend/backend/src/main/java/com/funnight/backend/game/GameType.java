package com.funnight.backend.game;

import java.util.Arrays;
import java.util.Optional;

/**
 * The six games of a full Fun Night session, in play order.
 * To add a new game: implement it, then flip {@code available} to true and give it a real name.
 */
public enum GameType {
    BLACKJACK("blackjack", "Blackjack", "Beat the dealer. Bet sips, not chips.", true),
    DARES("dares", "Brain Dares", "Do the dare or drink up, chicken.", true),
    HIGHER_LOWER("higher-lower", "Higher or Lower", "Guess the next card. Wrong? Drink the pile.", true),
    RING_OF_FIRE("ring-of-fire", "Ring of Fire", "Every card has a rule. Don't pull the last king.", true),
    GAME_5("game-5", "Mystery Game #5", "Still growing in the garden...", false),
    GAME_6("game-6", "Mystery Game #6", "Still growing in the garden...", false);

    private final String id;
    private final String displayName;
    private final String tagline;
    private final boolean available;

    GameType(String id, String displayName, String tagline, boolean available) {
        this.id = id;
        this.displayName = displayName;
        this.tagline = tagline;
        this.available = available;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getTagline() { return tagline; }
    public boolean isAvailable() { return available; }

    public static Optional<GameType> fromId(String id) {
        return Arrays.stream(values()).filter(g -> g.id.equals(id)).findFirst();
    }
}
