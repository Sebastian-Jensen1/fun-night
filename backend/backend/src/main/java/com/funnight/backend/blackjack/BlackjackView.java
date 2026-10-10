package com.funnight.backend.blackjack;

import java.util.List;

/**
 * What the table looks like to the players. While hands are in play the dealer's hole card is
 * withheld entirely (not just flagged), so it can't be read from the network tab.
 */
public record BlackjackView(
        BlackjackGame.Phase phase,
        int round,
        DealerView dealer,
        List<SeatView> seats,
        Integer currentPlayerId,
        int minBet,
        int maxBet,
        int cardsLeft
) {
    public record CardView(String rank, Suit suit) {
        static CardView of(Card c) {
            return new CardView(c.rank().getLabel(), c.suit());
        }
    }

    public record DealerView(List<CardView> cards, int hiddenCards, Integer value, boolean blackjack, boolean bust) {
    }

    public record SeatView(int playerId, String playerName, List<CardView> cards, int value, boolean soft,
                           int bet, boolean doubled, boolean canDouble, Seat.Status status,
                           Seat.Outcome outcome, int sipsToDrink, int sipsToGive) {
        static SeatView of(Seat s) {
            Hand h = s.getHand();
            return new SeatView(s.getPlayer().getId(), s.getPlayer().getName(), cardsOf(h), h.value(), h.isSoft(),
                    s.getBet(), s.isDoubled(), s.canDouble(), s.getStatus(), s.getOutcome(),
                    s.getSipsToDrink(), s.getSipsToGive());
        }
    }

    public static BlackjackView of(BlackjackGame game) {
        Hand dealer = game.getDealer();
        DealerView dealerView;
        if (game.getPhase() == BlackjackGame.Phase.PLAYER_TURNS) {
            Card up = dealer.getCards().get(0);
            dealerView = new DealerView(List.of(CardView.of(up)), dealer.size() - 1, up.rank().getValue(), false, false);
        } else {
            dealerView = new DealerView(cardsOf(dealer), 0, dealer.size() == 0 ? null : dealer.value(),
                    dealer.isBlackjack(), dealer.isBust());
        }
        Seat current = game.getCurrentSeat();
        return new BlackjackView(
                game.getPhase(),
                game.getRound(),
                dealerView,
                game.getSeats().stream().map(SeatView::of).toList(),
                current == null ? null : current.getPlayer().getId(),
                BlackjackGame.MIN_BET,
                BlackjackGame.MAX_BET,
                game.cardsLeft());
    }

    private static List<CardView> cardsOf(Hand hand) {
        return hand.getCards().stream().map(CardView::of).toList();
    }
}
