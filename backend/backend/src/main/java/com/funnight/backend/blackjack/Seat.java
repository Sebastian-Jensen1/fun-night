package com.funnight.backend.blackjack;

import com.funnight.backend.session.Player;

/** One player's hand and bet for the current round. */
public class Seat {
    public enum Status { PLAYING, STOOD, BUST, BLACKJACK }
    public enum Outcome { BLACKJACK, WIN, PUSH, LOSE, BUST }

    private final Player player;
    private final Hand hand = new Hand();
    private int bet;
    private boolean doubled;
    private Status status = Status.PLAYING;
    private Outcome outcome;
    private int sipsToDrink;
    private int sipsToGive;

    Seat(Player player, int bet) {
        this.player = player;
        this.bet = bet;
    }

    public Player getPlayer() { return player; }
    public Hand getHand() { return hand; }
    public int getBet() { return bet; }
    public boolean isDoubled() { return doubled; }
    public Status getStatus() { return status; }
    public Outcome getOutcome() { return outcome; }
    public int getSipsToDrink() { return sipsToDrink; }
    public int getSipsToGive() { return sipsToGive; }

    public boolean canDouble() {
        return status == Status.PLAYING && hand.size() == 2 && !doubled;
    }

    void setStatus(Status status) { this.status = status; }

    void doubleDown() {
        bet *= 2;
        doubled = true;
    }

    void settle(Outcome outcome) {
        this.outcome = outcome;
        switch (outcome) {
            case BLACKJACK -> sipsToGive = (bet * 3 + 1) / 2; // 3:2, rounded up
            case WIN -> sipsToGive = bet;
            case LOSE, BUST -> sipsToDrink = bet;
            case PUSH -> { }
        }
        player.drink(sipsToDrink);
        player.give(sipsToGive);
    }
}
