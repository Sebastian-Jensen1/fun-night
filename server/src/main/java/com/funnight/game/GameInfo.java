package com.funnight.game;

public record GameInfo(String id, String name, String tagline, int minPlayers, int order) {
    public static GameInfo of(GameType type) {
        return new GameInfo(type.getId(), type.getDisplayName(), type.getTagline(), type.getMinPlayers(), type.ordinal() + 1);
    }
}
