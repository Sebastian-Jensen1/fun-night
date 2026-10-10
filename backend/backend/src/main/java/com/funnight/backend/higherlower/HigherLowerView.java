package com.funnight.backend.higherlower;

import com.funnight.backend.cards.CardView;

public record HigherLowerView(CardView currentCard, int currentPlayerId, String currentPlayerName, int streak,
                              int pile, int sipsAtStake, boolean canPass, int streakToPass, int maxSips,
                              int cardsLeft, Result lastResult) {

    public record Result(int playerId, String playerName, HigherLowerGame.Guess guess, CardView from,
                         CardView drawn, boolean correct, boolean passed, int sipsDrunk) {
    }

    public static HigherLowerView of(HigherLowerGame g) {
        HigherLowerGame.LastResult r = g.getLastResult();
        Result result = r == null ? null : new Result(r.playerId(), r.playerName(), r.guess(), CardView.of(r.from()),
                r.drawn() == null ? null : CardView.of(r.drawn()), r.correct(), r.passed(), r.sipsDrunk());
        return new HigherLowerView(CardView.of(g.getCurrent()), g.getCurrentPlayer().getId(),
                g.getCurrentPlayer().getName(), g.getStreak(), g.getPile(), g.sipsAtStake(), g.canPass(),
                HigherLowerGame.STREAK_TO_PASS, HigherLowerGame.MAX_SIPS, g.cardsLeft(), result);
    }
}
