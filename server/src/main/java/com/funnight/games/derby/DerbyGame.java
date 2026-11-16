package com.funnight.games.derby;

import com.funnight.cards.Card;
import com.funnight.cards.CardView;
import com.funnight.cards.Deck;
import com.funnight.cards.Rank;
import com.funnight.cards.Rng;
import com.funnight.cards.Suit;
import com.funnight.common.ApiException;
import com.funnight.game.Game;
import com.funnight.game.Progress;
import com.funnight.room.Actor;
import com.funnight.room.Player;
import com.funnight.room.Room;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Derby: the four aces are horses. Everyone backs a suit with a bet in sips, then cards are
 * flipped one at a time and the ace of the flipped suit moves a step forward. Along the track
 * lie face-down hurdle cards: when every horse has passed one, it is turned over and that suit
 * stumbles a step back. First ace over the line wins: its backers hand out double their bet,
 * everyone else drinks theirs.
 */
public class DerbyGame implements Game {
    public static final int MIN_BET = 1;
    public static final int MAX_BET = 5;
    public static final int TRACK_LENGTH = 6;
    public static final int HURDLES = TRACK_LENGTH - 1;
    public static final int SUGGESTED_RACES = 3;

    public enum Phase { BETTING, RACING, FINISHED }

    public record Bet(Suit suit, int sips) implements Serializable {
    }

    public record Payout(int playerId, String playerName, Suit suit, int bet, int sipsToDrink, int sipsToGive)
            implements Serializable {
    }

    private final Room room;
    private final LinkedHashMap<Integer, Bet> bets = new LinkedHashMap<>();
    private final EnumMap<Suit, Integer> positions = new EnumMap<>(Suit.class);
    private final ArrayList<Card> hurdles = new ArrayList<>();
    private final ArrayList<Payout> payouts = new ArrayList<>();
    private ArrayDeque<Card> deck = new ArrayDeque<>();
    private Phase phase = Phase.BETTING;
    private int hurdlesRevealed;
    private int flips;
    private int racesFinished;
    private Card lastFlip;
    private Suit lastStumble;
    private Suit winner;

    public DerbyGame(Room room) {
        this.room = room;
        resetTrack();
    }

    /** Places or changes a bet while the bookie is open. */
    public void bet(int playerId, Suit suit, int sips) {
        if (phase == Phase.RACING) throw ApiException.rule("The race is on. No more bets!");
        if (phase == Phase.FINISHED) throw ApiException.rule("Line up a new race before betting again.");
        room.player(playerId);
        if (sips < MIN_BET || sips > MAX_BET) {
            throw ApiException.badRequest("Bets are between " + MIN_BET + " and " + MAX_BET + " sips.");
        }
        bets.put(playerId, new Bet(suit, sips));
    }

    /** Starts the race with a freshly shuffled deck. Players without a bet sit this one out. */
    public void start() {
        start(shuffledRaceDeck());
    }

    /** As {@link #start()}, but with a given deck (hurdle cards first), for tests. */
    public void start(List<Card> raceDeck) {
        if (phase != Phase.BETTING) throw ApiException.rule("The race has already started.");
        if (bets.isEmpty()) throw ApiException.rule("Somebody has to place a bet first.");
        deck = new ArrayDeque<>(raceDeck);
        for (int i = 0; i < HURDLES; i++) hurdles.add(deck.pop());
        phase = Phase.RACING;
    }

    /** Turns over the next card and moves the horses. */
    public void flip() {
        if (phase != Phase.RACING) throw ApiException.rule("There is no race running.");
        lastFlip = deck.pop();
        lastStumble = null;
        flips++;
        Suit suit = lastFlip.suit();
        positions.merge(suit, 1, Integer::sum);
        if (positions.get(suit) >= TRACK_LENGTH) {
            finish(suit);
            return;
        }
        // A hurdle is turned over once every horse has reached its row; that suit falls back a step.
        if (hurdlesRevealed < HURDLES && Collections.min(positions.values()) >= hurdlesRevealed + 1) {
            lastStumble = hurdles.get(hurdlesRevealed).suit();
            hurdlesRevealed++;
            positions.merge(lastStumble, -1, Integer::sum);
        }
    }

    private void finish(Suit winningSuit) {
        winner = winningSuit;
        phase = Phase.FINISHED;
        racesFinished++;
        bets.forEach((playerId, bet) -> {
            Player player = room.player(playerId);
            boolean won = bet.suit() == winningSuit;
            int toDrink = won ? 0 : bet.sips();
            int toGive = won ? bet.sips() * 2 : 0;
            player.drink(toDrink);
            room.award(player, toGive);
            payouts.add(new Payout(playerId, player.getName(), bet.suit(), bet.sips(), toDrink, toGive));
        });
    }

    /** Clears the track for another race. */
    public void newRace() {
        if (phase != Phase.FINISHED) throw ApiException.rule("Finish this race first.");
        resetTrack();
    }

    private void resetTrack() {
        for (Suit suit : Suit.values()) positions.put(suit, 0);
        bets.clear();
        hurdles.clear();
        payouts.clear();
        deck = new ArrayDeque<>();
        hurdlesRevealed = 0;
        flips = 0;
        lastFlip = null;
        lastStumble = null;
        winner = null;
        phase = Phase.BETTING;
    }

    /** The 48 cards that aren't aces. There are always enough for somebody to cross the line. */
    private static List<Card> shuffledRaceDeck() {
        List<Card> cards = new ArrayList<>(Deck.fullDeck());
        cards.removeIf(card -> card.rank() == Rank.ACE);
        Collections.shuffle(cards, Rng.SECURE);
        return cards;
    }

    public Phase getPhase() { return phase; }
    public Map<Suit, Integer> getPositions() { return positions; }
    public Suit getWinner() { return winner; }
    public Suit getLastStumble() { return lastStumble; }
    public int getHurdlesRevealed() { return hurdlesRevealed; }
    public List<Payout> getPayouts() { return payouts; }
    public Map<Integer, Bet> getBets() { return bets; }

    @Override
    public Progress progress() {
        return new Progress(racesFinished, SUGGESTED_RACES, "races");
    }

    // ---- view --------------------------------------------------------------------------------

    public record BetView(int playerId, String playerName, Suit suit, int sips) {
    }

    /** @param hurdles one entry per hurdle row; null until that hurdle has been turned over */
    public record View(Phase phase, int trackLength, Map<Suit, Integer> positions, List<CardView> hurdles,
                       CardView lastFlip, Suit lastStumble, int flips, List<BetView> bets, Suit winner,
                       List<Payout> payouts, int minBet, int maxBet, int race) {
    }

    @Override
    public View view(Actor viewer) {
        List<CardView> hurdleViews = new ArrayList<>();
        for (int i = 0; i < HURDLES; i++) {
            hurdleViews.add(i < hurdlesRevealed ? CardView.of(hurdles.get(i)) : null);
        }
        List<BetView> betViews = new ArrayList<>();
        bets.forEach((playerId, bet) ->
                betViews.add(new BetView(playerId, room.player(playerId).getName(), bet.suit(), bet.sips())));
        return new View(phase, TRACK_LENGTH, new EnumMap<>(positions), hurdleViews, CardView.of(lastFlip), lastStumble,
                flips, betViews, winner, List.copyOf(payouts), MIN_BET, MAX_BET,
                phase == Phase.FINISHED ? racesFinished : racesFinished + 1);
    }
}
