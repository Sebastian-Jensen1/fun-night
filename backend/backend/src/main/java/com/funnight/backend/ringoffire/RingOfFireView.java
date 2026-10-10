package com.funnight.backend.ringoffire;

import com.funnight.backend.cards.CardView;
import com.funnight.backend.session.Player;

import java.util.Arrays;
import java.util.List;

public record RingOfFireView(RingOfFireGame.Phase phase, int currentPlayerId, String currentPlayerName,
                             CardView currentCard, RuleView rule, int cardsLeft, int kingsDrawn, int kingsTotal,
                             int cupSips, String questionMaster, List<List<String>> mates, Result lastResult,
                             List<RuleView> rules) {

    public record RuleView(String rank, String title, String description, RingRule.Target target) {
        static RuleView of(RingRule r) {
            return new RuleView(r.getRank().getLabel(), r.getTitle(), r.getDescription(), r.getTarget());
        }
    }

    public record Result(int playerId, String playerName, CardView card, String ruleTitle,
                         List<RingOfFireGame.Drink> drinks) {
    }

    private static final List<RuleView> ALL_RULES = Arrays.stream(RingRule.values()).map(RuleView::of).toList();

    public static RingOfFireView of(RingOfFireGame g) {
        RingOfFireGame.LastResult r = g.getLastResult();
        boolean resolving = g.getPhase() == RingOfFireGame.Phase.RESOLVE;
        return new RingOfFireView(
                g.getPhase(),
                g.getCurrentPlayer().getId(),
                g.getCurrentPlayer().getName(),
                resolving ? CardView.of(g.getCurrentCard()) : null,
                resolving ? RuleView.of(RingRule.of(g.getCurrentCard().rank())) : null,
                g.cardsLeft(),
                g.getKingsDrawn(),
                RingOfFireGame.KINGS,
                g.getCupSips(),
                g.getQuestionMaster() == null ? null : g.getQuestionMaster().getName(),
                g.matePairs().stream().map(pair -> pair.stream().map(Player::getName).toList()).toList(),
                r == null ? null : new Result(r.playerId(), r.playerName(), CardView.of(r.card()), r.rule().getTitle(), r.drinks()),
                ALL_RULES);
    }
}
