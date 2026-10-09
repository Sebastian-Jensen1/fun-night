package com.funnight.backend.blackjack;

import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.exception.InvalidRequestException;
import com.funnight.backend.session.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Drinking blackjack: every player bets sips against the dealer. Lose and you drink your bet,
 * win and you hand your bet out to the table. Dealer stands on all 17s and peeks for blackjack.
 * Not thread-safe; callers hold the session lock.
 */
public class BlackjackGame {
    public static final int MIN_BET = 1;
    public static final int MAX_BET = 5;
    private static final int DEALER_STANDS_ON = 17;

    public enum Phase { BETTING, PLAYER_TURNS, ROUND_OVER }
    public enum Action { HIT, STAND, DOUBLE }

    private final Shoe shoe;
    private Phase phase = Phase.BETTING;
    private int round;
    private Hand dealer = new Hand();
    private List<Seat> seats = new ArrayList<>();
    private int currentSeat = -1;

    public BlackjackGame(Shoe shoe) {
        this.shoe = shoe;
    }

    public Phase getPhase() { return phase; }
    public int getRound() { return round; }
    public Hand getDealer() { return dealer; }
    public List<Seat> getSeats() { return Collections.unmodifiableList(seats); }
    public int cardsLeft() { return shoe.remaining(); }

    public Seat getCurrentSeat() {
        return phase == Phase.PLAYER_TURNS ? seats.get(currentSeat) : null;
    }

    /** Takes every player's bet and deals a new round. */
    public void deal(List<Player> players, Map<Integer, Integer> bets) {
        if (phase == Phase.PLAYER_TURNS) {
            throw new GameRuleException("Finish the current round before dealing again.");
        }
        List<Seat> newSeats = new ArrayList<>();
        for (Player player : players) {
            Integer bet = bets.get(player.getId());
            if (bet == null || bet < MIN_BET || bet > MAX_BET) {
                throw new InvalidRequestException(player.getName() + " must bet between " + MIN_BET + " and " + MAX_BET + " sips.");
            }
            newSeats.add(new Seat(player, bet));
        }
        if (bets.size() != players.size()) {
            throw new InvalidRequestException("Bets must be placed for exactly the players at the table.");
        }

        if (shoe.needsReshuffle()) shoe.shuffle();
        round++;
        seats = newSeats;
        dealer = new Hand();
        for (int i = 0; i < 2; i++) {
            for (Seat seat : seats) seat.getHand().add(shoe.draw());
            dealer.add(shoe.draw());
        }
        for (Seat seat : seats) {
            if (seat.getHand().isBlackjack()) seat.setStatus(Seat.Status.BLACKJACK);
        }

        phase = Phase.PLAYER_TURNS;
        if (dealer.isBlackjack()) {
            // Dealer peeked and has blackjack: nobody gets to act.
            settle();
            return;
        }
        currentSeat = -1;
        moveToNextSeat();
    }

    public void act(int playerId, Action action) {
        if (phase != Phase.PLAYER_TURNS) {
            throw new GameRuleException("There is no hand in play. Place your bets first.");
        }
        Seat seat = seats.get(currentSeat);
        if (seat.getPlayer().getId() != playerId) {
            throw new GameRuleException("It's " + seat.getPlayer().getName() + "'s turn.");
        }
        Hand hand = seat.getHand();
        switch (action) {
            case HIT -> {
                hand.add(shoe.draw());
                if (hand.isBust()) seat.setStatus(Seat.Status.BUST);
                else if (hand.value() == 21) seat.setStatus(Seat.Status.STOOD);
            }
            case STAND -> seat.setStatus(Seat.Status.STOOD);
            case DOUBLE -> {
                if (!seat.canDouble()) throw new GameRuleException("You can only double down on your first two cards.");
                seat.doubleDown();
                hand.add(shoe.draw());
                seat.setStatus(hand.isBust() ? Seat.Status.BUST : Seat.Status.STOOD);
            }
        }
        if (seat.getStatus() != Seat.Status.PLAYING) {
            moveToNextSeat();
        }
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

    private void playDealer() {
        boolean anyoneStanding = seats.stream().anyMatch(s -> s.getStatus() == Seat.Status.STOOD);
        if (!anyoneStanding) return;
        while (dealer.value() < DEALER_STANDS_ON) {
            dealer.add(shoe.draw());
        }
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
        }
        currentSeat = -1;
        phase = Phase.ROUND_OVER;
    }
}
