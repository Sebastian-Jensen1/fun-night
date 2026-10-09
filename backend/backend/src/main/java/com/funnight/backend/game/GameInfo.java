package com.funnight.backend.game;

public record GameInfo(String id, String name, String tagline, boolean available, int order) {
    public static GameInfo of(GameType type) {
        return new GameInfo(type.getId(), type.getDisplayName(), type.getTagline(), type.isAvailable(), type.ordinal() + 1);
    }
}
