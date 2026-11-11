package com.funnight.games.guilty;

import com.funnight.cards.Rng;
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
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Guilty!: a "who's most likely to..." prompt comes up and everyone votes in secret for the
 * player it fits best. Whoever gets the most votes is guilty and drinks one sip per vote
 * (capped); ties all drink. Nobody ever sees who voted for whom.
 */
public class GuiltyGame implements Game {
    public static final int MAX_SIPS = 5;
    public static final int SUGGESTED_ROUNDS = 6;

    public enum Phase { VOTING, VERDICT }

    public record Tally(int playerId, String playerName, int votes, boolean guilty) implements Serializable {
    }

    public record Verdict(String prompt, ArrayList<Tally> tally, int sips, int votesCast) implements Serializable {
    }

    private final Room room;
    private final ArrayList<String> prompts;
    private final ArrayDeque<String> deck = new ArrayDeque<>();
    /** voter id -> the player they accuse */
    private final LinkedHashMap<Integer, Integer> votes = new LinkedHashMap<>();
    private Phase phase = Phase.VOTING;
    private int round = 1;
    private String prompt;
    private Verdict verdict;

    public GuiltyGame(Room room, List<String> prompts) {
        if (prompts.isEmpty()) throw new IllegalArgumentException("There are no prompts.");
        this.room = room;
        this.prompts = new ArrayList<>(prompts);
        this.prompt = nextPrompt();
    }

    /** Casts or changes a vote. The verdict is read as soon as the last player has voted. */
    public void vote(int voterId, int accusedId) {
        if (phase != Phase.VOTING) throw ApiException.rule("The votes are in. Start the next round first.");
        room.player(voterId);
        room.player(accusedId);
        votes.put(voterId, accusedId);
        if (votes.size() == room.getPlayers().size()) readVerdict();
    }

    /** Reads the verdict without waiting for everyone, e.g. when a phone has gone quiet. */
    public void closeVoting() {
        if (phase != Phase.VOTING) throw ApiException.rule("The verdict is already in.");
        if (votes.isEmpty()) throw ApiException.rule("Nobody has voted yet.");
        readVerdict();
    }

    private void readVerdict() {
        LinkedHashMap<Player, Integer> counts = new LinkedHashMap<>();
        room.getPlayers().forEach(p -> counts.put(p, 0));
        votes.values().forEach(accusedId -> counts.merge(room.player(accusedId), 1, Integer::sum));
        int most = Collections.max(counts.values());
        int sips = Math.min(most, MAX_SIPS);
        ArrayList<Tally> tally = new ArrayList<>();
        counts.forEach((player, count) -> {
            boolean guilty = count == most;
            if (guilty) player.drink(sips);
            tally.add(new Tally(player.getId(), player.getName(), count, guilty));
        });
        verdict = new Verdict(prompt, tally, sips, votes.size());
        phase = Phase.VERDICT;
    }

    public void nextRound() {
        if (phase != Phase.VERDICT) throw ApiException.rule("Finish voting first.");
        votes.clear();
        prompt = nextPrompt();
        round++;
        phase = Phase.VOTING;
    }

    private String nextPrompt() {
        if (deck.isEmpty()) {
            List<String> shuffled = new ArrayList<>(prompts);
            Collections.shuffle(shuffled, Rng.SECURE);
            deck.addAll(shuffled);
        }
        return deck.pop();
    }

    public Phase getPhase() { return phase; }
    public int getRound() { return round; }
    public String getPrompt() { return prompt; }
    public Verdict getVerdict() { return verdict; }

    @Override
    public Progress progress() {
        return new Progress(phase == Phase.VERDICT ? round : round - 1, SUGGESTED_ROUNDS, "rounds");
    }

    /**
     * @param voted    who has voted so far (never whom they voted for)
     * @param yourVote only filled in on a player's own phone
     * @param verdict  only filled in once voting is closed
     */
    public record View(Phase phase, int round, String prompt, List<Integer> voted, Integer yourVote, int maxSips,
                       Verdict verdict) {
    }

    @Override
    public View view(Actor viewer) {
        Integer yourVote = viewer.playerId() == null ? null : votes.get(viewer.playerId());
        return new View(phase, round, prompt, List.copyOf(votes.keySet()), yourVote, MAX_SIPS,
                phase == Phase.VERDICT ? verdict : null);
    }
}
