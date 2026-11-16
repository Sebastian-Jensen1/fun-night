package com.funnight.game;

import java.util.Arrays;
import java.util.Optional;

/** The six games of a full Fun Night, in play order. */
public enum GameType {
    BLACKJACK("blackjack", "Blackjack", "Beat the dealer. Bet sips, not chips.", 1),
    DARES("dares", "Dare or Drink", "Mild or wild? Do the dare or pay in sips.", 1),
    HIGHER_LOWER("higher-lower", "Higher or Lower", "Guess the next card. Wrong? Drink the pile.", 1),
    RING_OF_FIRE("ring-of-fire", "Ring of Fire", "Every card has a rule. Don't pull the last king.", 1),
    GUILTY("guilty", "Guilty!", "Who's most likely to...? Vote in secret.", 3),
    DERBY("derby", "Derby", "Back a suit. Four aces race. Losers drink.", 1);

    private final String id;
    private final String displayName;
    private final String tagline;
    private final int minPlayers;

    GameType(String id, String displayName, String tagline, int minPlayers) {
        this.id = id;
        this.displayName = displayName;
        this.tagline = tagline;
        this.minPlayers = minPlayers;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getTagline() { return tagline; }
    /** Games that need a crowd are skipped in a full night when too few people are playing. */
    public int getMinPlayers() { return minPlayers; }

    public static Optional<GameType> fromId(String id) {
        return Arrays.stream(values()).filter(g -> g.id.equals(id)).findFirst();
    }
}
