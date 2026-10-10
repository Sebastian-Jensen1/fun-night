package com.funnight.backend.ringoffire;

import com.funnight.backend.cards.Card;
import com.funnight.backend.cards.Rank;
import com.funnight.backend.cards.Shoe;
import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.exception.InvalidRequestException;
import com.funnight.backend.session.Player;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Ring of Fire (King's Cup): a deck is spread in a ring and players take turns pulling a card,
 * each rank with its own rule. The game ends when the 4th king is pulled or the ring is empty.
 * Not thread-safe; callers hold the session lock.
 */
public class RingOfFireGame {
    public static final int SIPS_PER_PENALTY = 2;
    public static final int CUP_SIPS_PER_KING = 3; // kings 1-3 each pour this much; the 4th king drinks it all
    public static final int KINGS = 4;

    public enum Phase { DRAW, RESOLVE, OVER }

    public record Drink(int playerId, String playerName, int sips) {
    }

    public record LastResult(int playerId, String playerName, Card card, RingRule rule, List<Drink> drinks) {
    }

    private final Supplier<Shoe> shoeFactory;
    private final List<Player> players;
    private Shoe shoe;
    private Phase phase;
    private int currentPlayer;
    private Card currentCard;
    private int kingsDrawn;
    private int cupSips;
    private Player questionMaster;
    private final Map<Player, Set<Player>> mates = new LinkedHashMap<>();
    private LastResult lastResult;

    public RingOfFireGame(List<Player> players) {
        this(() -> new Shoe(1, new SecureRandom()), players);
    }

    public RingOfFireGame(Supplier<Shoe> shoeFactory, List<Player> players) {
        this.shoeFactory = shoeFactory;
        this.players = players;
        newRing();
    }

    public void draw(int playerId) {
        requireTurn(playerId, Phase.DRAW);
        currentCard = shoe.draw();
        if (currentCard.rank() == Rank.KING) kingsDrawn++;
        phase = Phase.RESOLVE;
    }

    /**
     * Applies the drawn card's rule. {@code targetId} names who drinks (or the new mate) for
     * rules that need one, and must be absent otherwise.
     */
    public void resolve(int playerId, Integer targetId) {
        Player drawer = requireTurn(playerId, Phase.RESOLVE);
        RingRule rule = RingRule.of(currentCard.rank());
        Player target = target(rule, drawer, targetId);
        Map<Player, Integer> drinks = new LinkedHashMap<>();

        switch (rule) {
            case WATERFALL -> players.forEach(p -> drinks.merge(p, 1, Integer::sum));
            case YOU, FLOOR, HEAVEN, RHYME, CATEGORIES -> drinkWithMates(drinks, target, SIPS_PER_PENALTY);
            case ME -> drinkWithMates(drinks, drawer, SIPS_PER_PENALTY);
            case MATE -> {
                mates.computeIfAbsent(drawer, p -> new LinkedHashSet<>()).add(target);
                mates.computeIfAbsent(target, p -> new LinkedHashSet<>()).add(drawer);
            }
            case QUESTION_MASTER -> questionMaster = drawer;
            case KINGS_CUP -> {
                if (kingsDrawn < KINGS) {
                    cupSips += CUP_SIPS_PER_KING;
                } else {
                    drinkWithMates(drinks, drawer, cupSips);
                    cupSips = 0;
                }
            }
            case GUYS, GIRLS, RULE -> { }
        }
        if (rule == RingRule.YOU) drawer.give(SIPS_PER_PENALTY);

        List<Drink> drinkList = new ArrayList<>();
        drinks.forEach((p, sips) -> {
            p.drink(sips);
            drinkList.add(new Drink(p.getId(), p.getName(), sips));
        });
        lastResult = new LastResult(drawer.getId(), drawer.getName(), currentCard, rule, drinkList);

        if (kingsDrawn >= KINGS || shoe.remaining() == 0) {
            phase = Phase.OVER;
        } else {
            phase = Phase.DRAW;
            currentPlayer = (currentPlayer + 1) % players.size();
        }
    }

    /** Lays out a fresh ring after the game is over. Mates and the Question Master reset too. */
    public void restart() {
        if (phase != Phase.OVER) throw new GameRuleException("The ring isn't finished yet.");
        currentPlayer = (currentPlayer + 1) % players.size();
        newRing();
    }

    private void newRing() {
        shoe = shoeFactory.get();
        phase = Phase.DRAW;
        currentCard = null;
        kingsDrawn = 0;
        cupSips = 0;
        questionMaster = null;
        mates.clear();
        lastResult = null;
    }

    /** The player drinks, and so does everyone they're mates with (once each). */
    private void drinkWithMates(Map<Player, Integer> drinks, Player player, int sips) {
        drinks.merge(player, sips, Integer::sum);
        for (Player mate : mates.getOrDefault(player, Set.of())) {
            drinks.merge(mate, sips, Integer::sum);
        }
    }

    private Player target(RingRule rule, Player drawer, Integer targetId) {
        if (rule.getTarget() == RingRule.Target.NONE) {
            if (targetId != null) throw new InvalidRequestException(rule.getTitle() + " doesn't need anyone picked.");
            return null;
        }
        if (targetId == null) throw new InvalidRequestException("Pick a player for " + rule.getTitle() + ".");
        Player target = players.stream().filter(p -> p.getId() == targetId).findFirst()
                .orElseThrow(() -> new InvalidRequestException("That player isn't in this game."));
        if (rule.getTarget() == RingRule.Target.OTHER && target == drawer && players.size() > 1) {
            throw new InvalidRequestException("Pick someone other than yourself.");
        }
        return target;
    }

    private Player requireTurn(int playerId, Phase expected) {
        if (phase == Phase.OVER) throw new GameRuleException("The ring is broken! Start a new one.");
        Player player = players.get(currentPlayer);
        if (player.getId() != playerId) throw new GameRuleException("It's " + player.getName() + "'s turn.");
        if (phase != expected) {
            throw new GameRuleException(expected == Phase.DRAW ? "Finish the card you pulled first." : "Pull a card first.");
        }
        return player;
    }

    public Phase getPhase() { return phase; }
    public Player getCurrentPlayer() { return players.get(currentPlayer); }
    public Card getCurrentCard() { return currentCard; }
    public int getKingsDrawn() { return kingsDrawn; }
    public int getCupSips() { return cupSips; }
    public Player getQuestionMaster() { return questionMaster; }
    public int cardsLeft() { return shoe.remaining(); }
    public LastResult getLastResult() { return lastResult; }

    /** Each mate pair once, in the order they were made. */
    public List<List<Player>> matePairs() {
        List<List<Player>> pairs = new ArrayList<>();
        mates.forEach((a, set) -> set.forEach(b -> {
            if (players.indexOf(a) < players.indexOf(b)) pairs.add(List.of(a, b));
        }));
        return Collections.unmodifiableList(pairs);
    }
}
