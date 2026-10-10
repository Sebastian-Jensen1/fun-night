package com.funnight.backend.dares;

public record DareView(DareGame.Phase phase, int turn, int currentPlayerId, String currentPlayerName,
                       String currentDare, int chickenSips, int completedSipsToGive, DareGame.LastResult lastResult) {
    public static DareView of(DareGame g) {
        return new DareView(g.getPhase(), g.getTurn(), g.getCurrentPlayer().getId(), g.getCurrentPlayer().getName(),
                g.getCurrentDare(), DareGame.CHICKEN_SIPS, DareGame.COMPLETED_SIPS_TO_GIVE, g.getLastResult());
    }
}
