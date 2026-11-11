package com.funnight.games.blackjack;

import com.funnight.cards.Card;
import com.funnight.cards.CardView;
import com.funnight.cards.Deck;
import com.funnight.common.ApiException;
import com.funnight.game.Game;
import com.funnight.game.Progress;
import com.funnight.room.Actor;
import com.funnight.room.Room;

import java.util.List;

/**
 * Drinking blackjack: every player bets sips against the dealer. Lose and you drink your bet,
 * win and you get your bet to hand out. Dealer stands on all 17s and peeks for blackjack.
 */
public class BlackjackGame implements Game {
    public static final int MIN_BET = 1;
    public static final int MAX_BET = 5;
    public static final int SUGGESTED_ROUNDS = 5;
    private static final int DEALER_STANDS_ON = 17;

    public enum Phase { BETTING, PLAYER_TURNS, ROUND_OVER }
    public enum Action { HIT, STAND, DOUBLE }

    private final Room room;
    private final Deck deck;
    private final List<Seat> seats;
    private Phase phase = Phase.BETTING;
    private int round;
    private Hand dealer = new Hand();
    private int currentSeat = -1;

    public BlackjackGame(Room room, Deck deck) {
        this.room = room;
        this.deck = deck;
        this.seats = room.getPlayers().stream().map(Seat::new).toList();
    }

    public Phase getPhase() { return phase; }
    public int getRound() { return round; }
    public Hand getDealer() { return dealer; }
    public List<Seat> getSeats() { return seats; }

    public Seat seatOf(int playerId) {
        return seats.stream().filter(s -> s.getPlayer().getId() == playerId).findFirst()
                .orElseThrow(() -> ApiException.badRequest("That player isn't in this game."));
    }

    /** Locks in a player's bet for the next deal. */
    public void bet(int playerId, int sips) {
        if (phase == Phase.PLAYER_TURNS) throw ApiException.rule("Bets are closed until this round is over.");
        Seat seat = seatOf(playerId);
        if (sips < MIN_BET || sips > MAX_BET) {
            throw ApiException.badRequest("Bets are between " + MIN_BET + " and " + MAX_BET + " sips.");
        }
        seat.placeBet(sips);
    }

    /** Deals a new round using every seat's current bet. */
    public void deal() {
        if (phase == Phase.PLAYER_TURNS) throw ApiException.rule("Finish the current round before dealing again.");
        if (deck.runningLow()) deck.shuffle();
        round++;
        dealer = new Hand();
        seats.forEach(Seat::newRound);
        for (int i = 0; i < 2; i++) {
            for (Seat seat : seats) seat.getHand().add(deck.draw());
            dealer.add(deck.draw());
        }
        for (Seat seat : seats) {
            if (seat.getHand().isBlackjack()) seat.setStatus(Seat.Status.BLACKJACK);
        }

        phase = Phase.PLAYER_TURNS;
        currentSeat = -1;
        if (dealer.isBlackjack()) {
            // Dealer peeked and has blackjack: nobody gets to act.
            settle();
            return;
        }
        moveToNextSeat();
    }

    public void act(int playerId, Action action) {
        if (phase != Phase.PLAYER_TURNS) throw ApiException.rule("There is no hand in play. Deal first.");
        Seat seat = seats.get(currentSeat);
        if (seat.getPlayer().getId() != playerId) {
            throw ApiException.rule("It's " + seat.getPlayer().getName() + "'s turn.");
        }
        Hand hand = seat.getHand();
        switch (action) {
            case HIT -> {
                hand.add(deck.draw());
                if (hand.isBust()) seat.setStatus(Seat.Status.BUST);
                else if (hand.value() == 21) seat.setStatus(Seat.Status.STOOD);
            }
            case STAND -> seat.setStatus(Seat.Status.STOOD);
            case DOUBLE -> {
                if (!seat.canDouble()) throw ApiException.rule("You can only double down on your first two cards.");
                seat.doubleDown();
                hand.add(deck.draw());
                seat.setStatus(hand.isBust() ? Seat.Status.BUST : Seat.Status.STOOD);
            }
        }
        if (seat.getStatus() != Seat.Status.PLAYING) moveToNextSeat();
    }

    private void moveToNextSeat() {
        for (int i = currentSeat + 1; i < seats.size(); i++) {
            if (seats.get(i).getStatus() == Seat.Status.PLAYING) {
                currentSeat = i;
                return;
            }
        }
        playDealer();
        settle();
    }

    /** The dealer only draws when somebody is still standing against them. */
    private void playDealer() {
        boolean anyoneStanding = seats.stream().anyMatch(s -> s.getStatus() == Seat.Status.STOOD);
        if (!anyoneStanding) return;
        while (dealer.value() < DEALER_STANDS_ON) dealer.add(deck.draw());
    }

    private void settle() {
        int dealerValue = dealer.value();
        boolean dealerBlackjack = dealer.isBlackjack();
        for (Seat seat : seats) {
            Hand hand = seat.getHand();
            Seat.Outcome outcome;
            if (seat.getStatus() == Seat.Status.BUST) {
                outcome = Seat.Outcome.BUST;
            } else if (seat.getStatus() == Seat.Status.BLACKJACK) {
                outcome = dealerBlackjack ? Seat.Outcome.PUSH : Seat.Outcome.BLACKJACK;
            } else if (dealerBlackjack) {
                outcome = Seat.Outcome.LOSE;
            } else if (dealer.isBust() || hand.value() > dealerValue) {
                outcome = Seat.Outcome.WIN;
            } else if (hand.value() == dealerValue) {
                outcome = Seat.Outcome.PUSH;
            } else {
                outcome = Seat.Outcome.LOSE;
            }
            seat.settle(outcome);
            seat.getPlayer().drink(seat.getSipsToDrink());
            room.award(seat.getPlayer(), seat.getSipsToGive());
        }
        currentSeat = -1;
        phase = Phase.ROUND_OVER;
    }

    @Override
    public Progress progress() {
        int finished = phase == Phase.PLAYER_TURNS ? round - 1 : round;
        return new Progress(finished, SUGGESTED_ROUNDS, "rounds");
    }

    // ---- view --------------------------------------------------------------------------------

    public record View(Phase phase, int round, DealerView dealer, List<SeatView> seats, Integer currentPlayerId,
                       int minBet, int maxBet) {
    }

    public record DealerView(List<CardView> cards, int hiddenCards, Integer value, boolean blackjack, boolean bust) {
    }

    public record SeatView(int playerId, String name, int bet, boolean ready, int stake, List<CardView> cards,
                           int value, boolean soft, boolean doubled, boolean canDouble, Seat.Status status,
                           Seat.Outcome outcome, int sipsToDrink, int sipsToGive) {
    }

    /**
     * While hands are in play the dealer's hole card is withheld entirely (not just flagged),
     * so it can't be read from the network tab.
     */
    @Override
    public View view(Actor viewer) {
        DealerView dealerView;
        if (phase == Phase.PLAYER_TURNS) {
            Card up = dealer.getCards().get(0);
            dealerView = new DealerView(List.of(CardView.of(up)), dealer.size() - 1, up.rank().getValue(), false, false);
        } else {
            dealerView = new DealerView(cards(dealer), 0, dealer.size() == 0 ? null : dealer.value(),
                    dealer.isBlackjack(), dealer.isBust());
        }
        List<SeatView> seatViews = seats.stream().map(s -> {
            Hand h = s.getHand();
            return new SeatView(s.getPlayer().getId(), s.getPlayer().getName(), s.getBet(), s.isReady(), s.getStake(),
                    cards(h), h.value(), h.isSoft(), s.isDoubled(), s.canDouble(), s.getStatus(), s.getOutcome(),
                    s.getSipsToDrink(), s.getSipsToGive());
        }).toList();
        Integer current = phase == Phase.PLAYER_TURNS ? seats.get(currentSeat).getPlayer().getId() : null;
        return new View(phase, round, dealerView, seatViews, current, MIN_BET, MAX_BET);
    }

    private static List<CardView> cards(Hand hand) {
        return hand.getCards().stream().map(CardView::of).toList();
    }
}
