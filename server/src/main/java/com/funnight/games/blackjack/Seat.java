package com.funnight.games.blackjack;

import com.funnight.room.Player;

import java.io.Serializable;

/** One player's bet and hand. The seat lives for the whole game so the bet is remembered between rounds. */
public class Seat implements Serializable {
    public enum Status { WAITING, PLAYING, STOOD, BUST, BLACKJACK }
    public enum Outcome { BLACKJACK, WIN, PUSH, LOSE, BUST }

    private final Player player;
    private int bet = BlackjackGame.MIN_BET;
    private boolean ready;
    private Hand hand = new Hand();
    private int stake;
    private boolean doubled;
    private Status status = Status.WAITING;
    private Outcome outcome;
    private int sipsToDrink;
    private int sipsToGive;

    Seat(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }
    /** The bet for the next deal. */
    public int getBet() { return bet; }
    /** True once the player has locked in a bet for the next deal. */
    public boolean isReady() { return ready; }
    public Hand getHand() { return hand; }
    /** What is riding on the hand in play (the bet, doubled if they doubled down). */
    public int getStake() { return stake; }
    public boolean isDoubled() { return doubled; }
    public Status getStatus() { return status; }
    public Outcome getOutcome() { return outcome; }
    public int getSipsToDrink() { return sipsToDrink; }
    public int getSipsToGive() { return sipsToGive; }

    public boolean canDouble() {
        return status == Status.PLAYING && hand.size() == 2 && !doubled;
    }

    void placeBet(int bet) {
        this.bet = bet;
        this.ready = true;
    }

    void newRound() {
        hand = new Hand();
        stake = bet;
        doubled = false;
        status = Status.PLAYING;
        outcome = null;
        sipsToDrink = 0;
        sipsToGive = 0;
    }

    void setStatus(Status status) { this.status = status; }

    void doubleDown() {
        stake *= 2;
        doubled = true;
    }

    void settle(Outcome outcome) {
        this.outcome = outcome;
        this.ready = false;
        switch (outcome) {
            case BLACKJACK -> sipsToGive = (stake * 3 + 1) / 2; // 3:2, rounded up
            case WIN -> sipsToGive = stake;
            case LOSE, BUST -> sipsToDrink = stake;
            case PUSH -> { }
        }
    }
}
