package com.funnight.games;

import com.funnight.cards.Card;
import com.funnight.cards.Rank;
import com.funnight.cards.Suit;
import com.funnight.common.ApiException;
import com.funnight.games.derby.DerbyGame;
import com.funnight.games.guilty.GuiltyGame;
import com.funnight.room.Actor;
import com.funnight.room.Room;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.funnight.TestRooms.card;
import static com.funnight.TestRooms.room;
import static org.junit.jupiter.api.Assertions.*;

/** Guilty! and Derby. */
class NewGamesTest {

    @Test
    void mostVotedPlayerDrinksOneSipPerVote() {
        Room room = room("Anna", "Bo", "Cy");
        GuiltyGame game = new GuiltyGame(room, List.of("prompt one", "prompt two"));
        game.vote(1, 2);
        game.vote(2, 3);
        assertEquals(GuiltyGame.Phase.VOTING, game.getPhase());
        game.vote(3, 2);
        assertEquals(GuiltyGame.Phase.VERDICT, game.getPhase(), "the last vote closes the round");
        assertEquals(2, room.player(2).getSipsDrunk());
        assertEquals(0, room.player(3).getSipsDrunk());
        assertEquals(0, room.player(1).getSipsDrunk());
    }

    @Test
    void tiedPlayersAllDrinkAndVotesCanBeChanged() {
        Room room = room("Anna", "Bo", "Cy");
        GuiltyGame game = new GuiltyGame(room, List.of("prompt"));
        game.vote(1, 3);
        game.vote(1, 2); // changes her mind
        game.vote(2, 1);
        game.closeVoting();
        assertEquals(1, room.player(1).getSipsDrunk());
        assertEquals(1, room.player(2).getSipsDrunk());
        assertEquals(0, room.player(3).getSipsDrunk());
        game.nextRound();
        assertEquals(2, game.getRound());
        assertThrows(ApiException.class, game::closeVoting, "nobody has voted in the new round");
    }

    @Test
    void votesStaySecret() {
        Room room = room("Anna", "Bo", "Cy");
        GuiltyGame game = new GuiltyGame(room, List.of("prompt"));
        game.vote(1, 2);
        GuiltyGame.View host = game.view(Actor.ofHost());
        assertEquals(List.of(1), host.voted());
        assertNull(host.yourVote());
        assertNull(host.verdict());
        assertEquals(2, game.view(Actor.ofPlayer(1)).yourVote());
        assertNull(game.view(Actor.ofPlayer(2)).yourVote());
    }

    /** Five hurdle cards first, then the flips. */
    private static List<Card> raceDeck(List<Suit> hurdles, Suit... flips) {
        List<Card> deck = new ArrayList<>();
        hurdles.forEach(suit -> deck.add(card(Rank.TWO, suit)));
        for (Suit suit : flips) deck.add(card(Rank.NINE, suit));
        return deck;
    }

    private static final List<Suit> CLUB_HURDLES = List.of(Suit.CLUBS, Suit.CLUBS, Suit.CLUBS, Suit.CLUBS, Suit.CLUBS);

    @Test
    void backersOfTheWinnerHandOutDoubleAndTheRestDrink() {
        Room room = room("Anna", "Bo", "Cy");
        DerbyGame game = new DerbyGame(room);
        game.bet(1, Suit.HEARTS, 2);
        game.bet(2, Suit.SPADES, 3);
        game.start(raceDeck(CLUB_HURDLES, Suit.HEARTS, Suit.HEARTS, Suit.HEARTS, Suit.HEARTS, Suit.HEARTS, Suit.HEARTS));
        for (int i = 0; i < 5; i++) game.flip();
        assertEquals(DerbyGame.Phase.RACING, game.getPhase());
        game.flip();
        assertEquals(DerbyGame.Phase.FINISHED, game.getPhase());
        assertEquals(Suit.HEARTS, game.getWinner());
        assertEquals(4, room.player(1).getPendingGive());
        assertEquals(3, room.player(2).getSipsDrunk());
        assertEquals(0, room.player(3).getSipsDrunk(), "Cy didn't bet and sits the race out");
        assertThrows(ApiException.class, game::flip);
        game.newRace();
        assertEquals(DerbyGame.Phase.BETTING, game.getPhase());
        assertTrue(game.getBets().isEmpty());
    }

    @Test
    void aHurdleTurnsOverWhenEveryHorseHasPassedItAndThatSuitStumbles() {
        Room room = room("Anna");
        DerbyGame game = new DerbyGame(room);
        game.bet(1, Suit.CLUBS, 1);
        game.start(raceDeck(CLUB_HURDLES, Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES, Suit.HEARTS));
        for (int i = 0; i < 3; i++) game.flip();
        assertEquals(0, game.getHurdlesRevealed(), "spades hasn't left the gate yet");
        game.flip();
        assertEquals(1, game.getHurdlesRevealed());
        assertEquals(Suit.CLUBS, game.getLastStumble());
        assertEquals(0, game.getPositions().get(Suit.CLUBS));
        assertEquals(1, game.getPositions().get(Suit.HEARTS));
        game.flip();
        assertNull(game.getLastStumble());
        assertEquals(1, game.getHurdlesRevealed(), "clubs is behind the next hurdle again");
    }

    @Test
    void derbyValidatesBetsAndPhases() {
        Room room = room("Anna");
        DerbyGame game = new DerbyGame(room);
        assertThrows(ApiException.class, game::start, "no bets yet");
        assertThrows(ApiException.class, () -> game.bet(1, Suit.HEARTS, 0));
        assertThrows(ApiException.class, () -> game.bet(9, Suit.HEARTS, 1));
        assertThrows(ApiException.class, game::flip);
        game.bet(1, Suit.HEARTS, 1);
        game.start();
        assertThrows(ApiException.class, () -> game.bet(1, Suit.SPADES, 1), "no bets once the race is on");
        int flips = 0;
        while (game.getPhase() == DerbyGame.Phase.RACING) {
            game.flip();
            flips++;
        }
        assertTrue(flips <= 43, "a real deck always produces a winner");
        assertNotNull(game.getWinner());
    }
}
