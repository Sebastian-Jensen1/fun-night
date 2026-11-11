package com.funnight.games.ringoffire;

import com.funnight.cards.Card;
import com.funnight.cards.CardView;
import com.funnight.cards.Deck;
import com.funnight.cards.Rank;
import com.funnight.common.ApiException;
import com.funnight.game.Game;
import com.funnight.game.Progress;
import com.funnight.room.Actor;
import com.funnight.room.Player;
import com.funnight.room.Room;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ring of Fire (King's Cup): a deck is spread in a ring and players take turns pulling a card,
 * each rank with its own rule. The ring breaks when the 4th king is pulled.
 */
public class RingOfFireGame implements Game {
    public static final int SIPS_PER_PENALTY = 2;
    public static final int CUP_SIPS_PER_KING = 3; // kings 1-3 each pour this much; the 4th king drinks it all
    public static final int KINGS = 4;
    public static final int RING_SIZE = 52;

    public enum Phase { DRAW, RESOLVE, OVER }

    public record Drink(int playerId, String playerName, int sips) implements Serializable {
    }

    public record LastResult(int playerId, String playerName, Card card, RingRule rule, ArrayList<Drink> drinks)
            implements Serializable {
    }

    private final Room room;
    private Deck deck;
    private Phase phase;
    private int currentPlayer;
    private Card currentCard;
    private int kingsDrawn;
    private int cupSips;
    private int ringsBroken;
    private Player questionMaster;
    private final LinkedHashMap<Player, LinkedHashSet<Player>> mates = new LinkedHashMap<>();
    private LastResult lastResult;

    public RingOfFireGame(Room room, Deck deck) {
        this.room = room;
        newRing(deck);
    }

    public void draw(int playerId) {
        requireTurn(playerId, Phase.DRAW);
        currentCard = deck.draw();
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
        List<Player> players = room.getPlayers();
        Map<Player, Integer> drinks = new LinkedHashMap<>();

        switch (rule) {
            case WATERFALL -> players.forEach(p -> drinks.merge(p, 1, Integer::sum));
            case YOU, FLOOR, HEAVEN, RHYME, CATEGORIES -> drinkWithMates(drinks, target, SIPS_PER_PENALTY);
            case ME -> drinkWithMates(drinks, drawer, SIPS_PER_PENALTY);
            case BEHIND -> drinkWithMates(drinks, neighbour(-1), SIPS_PER_PENALTY);
            case AHEAD -> drinkWithMates(drinks, neighbour(1), SIPS_PER_PENALTY);
            case MATE -> {
                // Solo there is nobody to pair up with; pairing a player with themselves would double their sips.
                if (target != drawer) {
                    mates.computeIfAbsent(drawer, p -> new LinkedHashSet<>()).add(target);
                    mates.computeIfAbsent(target, p -> new LinkedHashSet<>()).add(drawer);
                }
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
            case RULE -> { }
        }

        ArrayList<Drink> drinkList = new ArrayList<>();
        drinks.forEach((p, sips) -> {
            // On a "You" card the picked player's sips count as handed out by the drawer; their mates' don't.
            boolean pickedByDrawer = rule == RingRule.YOU && p == target && p != drawer;
            if (pickedByDrawer) room.give(drawer, p, sips);
            else p.drink(sips);
            drinkList.add(new Drink(p.getId(), p.getName(), sips));
        });
        lastResult = new LastResult(drawer.getId(), drawer.getName(), currentCard, rule, drinkList);
        currentCard = null;

        if (kingsDrawn >= KINGS || deck.remaining() == 0) {
            phase = Phase.OVER;
            ringsBroken++;
        } else {
            phase = Phase.DRAW;
            currentPlayer = (currentPlayer + 1) % players.size();
        }
    }

    /** Lays out a fresh ring after the last one broke. Mates and the Question Master reset too. */
    public void restart() {
        if (phase != Phase.OVER) throw ApiException.rule("The ring isn't broken yet.");
        currentPlayer = (currentPlayer + 1) % room.getPlayers().size();
        newRing(Deck.shuffled(1));
    }

    private void newRing(Deck fresh) {
        deck = fresh;
        phase = Phase.DRAW;
        currentCard = null;
        kingsDrawn = 0;
        cupSips = 0;
        questionMaster = null;
        mates.clear();
        lastResult = null;
    }

    private Player neighbour(int offset) {
        List<Player> players = room.getPlayers();
        return players.get(Math.floorMod(currentPlayer + offset, players.size()));
    }

    /** The player drinks, and so does everyone they're mates with (once each). */
    private void drinkWithMates(Map<Player, Integer> drinks, Player player, int sips) {
        if (sips <= 0) return;
        drinks.merge(player, sips, Integer::sum);
        for (Player mate : mates.getOrDefault(player, new LinkedHashSet<>())) {
            drinks.merge(mate, sips, Integer::sum);
        }
    }

    private Player target(RingRule rule, Player drawer, Integer targetId) {
        if (rule.getTarget() == RingRule.Target.NONE) {
            if (targetId != null) throw ApiException.badRequest(rule.getTitle() + " doesn't need anyone picked.");
            return null;
        }
        if (targetId == null) throw ApiException.badRequest("Pick a player for " + rule.getTitle() + ".");
        Player target = room.player(targetId);
        if (rule.getTarget() == RingRule.Target.OTHER && target == drawer && room.getPlayers().size() > 1) {
            throw ApiException.badRequest("Pick someone other than yourself.");
        }
        return target;
    }

    private Player requireTurn(int playerId, Phase expected) {
        if (phase == Phase.OVER) throw ApiException.rule("The ring is broken! Start a new one.");
        Player player = getCurrentPlayer();
        if (player.getId() != playerId) throw ApiException.rule("It's " + player.getName() + "'s turn.");
        if (phase != expected) {
            throw ApiException.rule(expected == Phase.DRAW ? "Finish the card you pulled first." : "Pull a card first.");
        }
        return player;
    }

    public Phase getPhase() { return phase; }
    public Player getCurrentPlayer() { return room.getPlayers().get(currentPlayer); }
    public Card getCurrentCard() { return currentCard; }
    public int getKingsDrawn() { return kingsDrawn; }
    public int getCupSips() { return cupSips; }
    public Player getQuestionMaster() { return questionMaster; }
    public int cardsLeft() { return deck.remaining(); }
    public LastResult getLastResult() { return lastResult; }

    /** Each mate pair once, in the order they were made. */
    public List<List<Player>> matePairs() {
        List<Player> players = room.getPlayers();
        List<List<Player>> pairs = new ArrayList<>();
        mates.forEach((a, set) -> set.forEach(b -> {
            if (players.indexOf(a) < players.indexOf(b)) pairs.add(List.of(a, b));
        }));
        return pairs;
    }

    @Override
    public Progress progress() {
        return ringsBroken > 0 ? new Progress(KINGS, KINGS, "kings") : new Progress(kingsDrawn, KINGS, "kings");
    }

    // ---- view --------------------------------------------------------------------------------

    public record RuleView(String rank, String title, String description, RingRule.Target target) {
        static RuleView of(RingRule r) {
            return new RuleView(r.getRank().getLabel(), r.getTitle(), r.getDescription(), r.getTarget());
        }
    }

    public record ResultView(int playerId, String playerName, CardView card, String ruleTitle, List<Drink> drinks) {
    }

    public record View(Phase phase, int currentPlayerId, String currentPlayerName, CardView currentCard, RuleView rule,
                       int cardsLeft, int ringSize, int kingsDrawn, int kingsTotal, int cupSips, int cupSipsPerKing,
                       String questionMaster, List<List<String>> mates, ResultView lastResult, List<RuleView> rules) {
    }

    private static final List<RuleView> ALL_RULES = Arrays.stream(RingRule.values()).map(RuleView::of).toList();

    @Override
    public View view(Actor viewer) {
        LastResult r = lastResult;
        boolean resolving = phase == Phase.RESOLVE;
        return new View(
                phase,
                getCurrentPlayer().getId(),
                getCurrentPlayer().getName(),
                resolving ? CardView.of(currentCard) : null,
                resolving ? RuleView.of(RingRule.of(currentCard.rank())) : null,
                cardsLeft(),
                RING_SIZE,
                kingsDrawn,
                KINGS,
                cupSips,
                CUP_SIPS_PER_KING,
                questionMaster == null ? null : questionMaster.getName(),
                matePairs().stream().map(pair -> pair.stream().map(Player::getName).toList()).toList(),
                r == null ? null : new ResultView(r.playerId(), r.playerName(), CardView.of(r.card()), r.rule().getTitle(), r.drinks()),
                ALL_RULES);
    }
}
