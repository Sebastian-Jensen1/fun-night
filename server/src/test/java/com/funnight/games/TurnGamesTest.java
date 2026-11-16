package com.funnight.games;

import com.funnight.cards.Card;
import com.funnight.cards.Deck;
import com.funnight.cards.Rank;
import com.funnight.common.ApiException;
import com.funnight.games.dares.DareGame;
import com.funnight.games.higherlower.HigherLowerGame;
import com.funnight.games.higherlower.HigherLowerGame.Guess;
import com.funnight.room.Room;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.funnight.TestRooms.card;
import static com.funnight.TestRooms.room;
import static org.junit.jupiter.api.Assertions.*;

/** Dare or Drink and Higher or Lower. */
class TurnGamesTest {

    @Test
    void doingADareWinsSipsAndChickeningOutDrinks() {
        Room room = room("Anna", "Bo");
        DareGame game = new DareGame(room, List.of("mild one"), List.of("wild one"));

        game.draw(1, DareGame.Level.WILD);
        assertEquals("wild one", game.getDare());
        game.resolve(1, true);
        assertEquals(3, room.player(1).getPendingGive());

        game.draw(2, DareGame.Level.MILD);
        game.resolve(2, false);
        assertEquals(2, room.player(2).getSipsDrunk());
        assertEquals(1, game.getCurrentPlayer().getId(), "the turn wraps around");
    }

    @Test
    void daresDoNotRepeatUntilTheDeckIsUsedUp() {
        Room room = room("Anna");
        DareGame game = new DareGame(room, List.of("a", "b", "c", "d"), List.of("w"));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            game.draw(1, DareGame.Level.MILD);
            seen.add(game.getDare());
            game.resolve(1, true);
        }
        assertEquals(4, seen.size());
    }

    @Test
    void daresEnforceTurnAndPhase() {
        Room room = room("Anna", "Bo");
        DareGame game = new DareGame(room, List.of("a"), List.of("w"));
        assertThrows(ApiException.class, () -> game.draw(2, DareGame.Level.MILD));
        assertThrows(ApiException.class, () -> game.resolve(1, true), "nothing drawn yet");
        game.draw(1, DareGame.Level.MILD);
        assertThrows(ApiException.class, () -> game.draw(1, DareGame.Level.MILD), "finish the dare first");
    }

    private static HigherLowerGame higherLower(Room room, Rank... order) {
        List<Card> cards = Arrays.stream(order).map(r -> card(r)).toList();
        return new HigherLowerGame(room, Deck.stacked(cards));
    }

    @Test
    void rightGuessesGrowThePileAndAWrongOneDrinksIt() {
        Room room = room("Anna", "Bo");
        HigherLowerGame game = higherLower(room, Rank.FIVE, Rank.NINE, Rank.KING, Rank.TWO);
        game.guess(1, Guess.HIGHER);
        game.guess(1, Guess.HIGHER);
        assertEquals(3, game.getPile());
        game.guess(1, Guess.HIGHER); // a 2 comes up
        assertEquals(3, room.player(1).getSipsDrunk());
        assertEquals(1, game.getPile());
        assertEquals(2, game.getCurrentPlayer().getId());
    }

    @Test
    void sameRankCountsAsWrongAndAcesAreHigh() {
        Room room = room("Anna");
        HigherLowerGame game = higherLower(room, Rank.KING, Rank.ACE, Rank.ACE);
        game.guess(1, Guess.HIGHER);
        assertTrue(game.getLastResult().correct(), "ace beats king");
        game.guess(1, Guess.HIGHER);
        assertFalse(game.getLastResult().correct(), "ace on ace is wrong either way");
    }

    @Test
    void pileIsCappedAndCanBePassedAfterAStreak() {
        Room room = room("Anna", "Bo");
        HigherLowerGame game = higherLower(room, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN,
                Rank.EIGHT, Rank.TWO);
        assertThrows(ApiException.class, () -> game.pass(1), "no streak yet");
        for (int i = 0; i < 6; i++) game.guess(1, Guess.HIGHER);
        assertEquals(7, game.getPile());
        assertEquals(HigherLowerGame.MAX_SIPS, game.sipsAtStake());
        game.pass(1);
        assertEquals(2, game.getCurrentPlayer().getId());
        assertEquals(7, game.getPile(), "the pile travels with the pass");
        game.guess(2, Guess.HIGHER); // a 2 comes up
        assertEquals(HigherLowerGame.MAX_SIPS, room.player(2).getSipsDrunk());
        assertEquals(0, room.player(1).getSipsDrunk());
    }
}
