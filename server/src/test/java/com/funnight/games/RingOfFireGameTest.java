package com.funnight.games;

import com.funnight.cards.Card;
import com.funnight.cards.Deck;
import com.funnight.cards.Rank;
import com.funnight.common.ApiException;
import com.funnight.games.ringoffire.RingOfFireGame;
import com.funnight.room.Room;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.funnight.TestRooms.card;
import static com.funnight.TestRooms.room;
import static org.junit.jupiter.api.Assertions.*;

class RingOfFireGameTest {

    private static RingOfFireGame game(Room room, Rank... order) {
        List<Card> cards = Arrays.stream(order).map(r -> card(r)).toList();
        return new RingOfFireGame(room, Deck.stacked(cards));
    }

    private static void pull(RingOfFireGame game, int playerId, Integer targetId) {
        game.draw(playerId);
        game.resolve(playerId, targetId);
    }

    @Test
    void youCardMakesThePickedPlayerDrinkAndCountsAsGiven() {
        Room room = room("Anna", "Bo", "Cy");
        RingOfFireGame game = game(room, Rank.TWO, Rank.THREE, Rank.NINE);
        pull(game, 1, 2);
        assertEquals(2, room.player(2).getSipsDrunk());
        assertEquals(2, room.player(1).getSipsGiven());
        pull(game, 2, null); // Me
        assertEquals(4, room.player(2).getSipsDrunk());
    }

    @Test
    void neighbourCardsFollowTheTurnOrder() {
        Room room = room("Anna", "Bo", "Cy");
        RingOfFireGame game = game(room, Rank.FIVE, Rank.SIX, Rank.NINE);
        pull(game, 1, null); // Behind You: the player before Anna is Cy
        assertEquals(2, room.player(3).getSipsDrunk());
        pull(game, 2, null); // Up Next: the player after Bo is Cy
        assertEquals(4, room.player(3).getSipsDrunk());
    }

    @Test
    void matesDrinkTogether() {
        Room room = room("Anna", "Bo", "Cy");
        RingOfFireGame game = game(room, Rank.EIGHT, Rank.THREE, Rank.NINE);
        pull(game, 1, 2); // Anna and Bo are mates
        pull(game, 2, null); // Bo: Me
        assertEquals(2, room.player(1).getSipsDrunk());
        assertEquals(2, room.player(2).getSipsDrunk());
        assertEquals(0, room.player(3).getSipsDrunk());
        assertEquals(1, game.matePairs().size());
    }

    @Test
    void aSoloPlayerCannotBecomeTheirOwnMate() {
        Room room = room("Anna");
        RingOfFireGame game = game(room, Rank.EIGHT, Rank.THREE, Rank.NINE);
        pull(game, 1, 1);
        assertTrue(game.matePairs().isEmpty());
        pull(game, 1, null); // Me: 2 sips, not 4
        assertEquals(2, room.player(1).getSipsDrunk());
    }

    @Test
    void fourthKingDrinksTheCupAndBreaksTheRing() {
        Room room = room("Anna", "Bo");
        RingOfFireGame game = game(room, Rank.KING, Rank.KING, Rank.KING, Rank.KING, Rank.TWO);
        pull(game, 1, null);
        pull(game, 2, null);
        pull(game, 1, null);
        assertEquals(9, game.getCupSips());
        pull(game, 2, null);
        assertEquals(9, room.player(2).getSipsDrunk());
        assertEquals(RingOfFireGame.Phase.OVER, game.getPhase());
        assertThrows(ApiException.class, () -> game.draw(1));
        game.restart();
        assertEquals(RingOfFireGame.Phase.DRAW, game.getPhase());
        assertEquals(0, game.getKingsDrawn());
        assertEquals(52, game.cardsLeft());
    }

    @Test
    void validatesTargets() {
        Room room = room("Anna", "Bo");
        RingOfFireGame game = game(room, Rank.TWO, Rank.THREE);
        game.draw(1);
        assertThrows(ApiException.class, () -> game.resolve(1, null), "You needs a target");
        assertThrows(ApiException.class, () -> game.resolve(1, 1), "can't pick yourself");
        assertThrows(ApiException.class, () -> game.resolve(1, 99));
        assertThrows(ApiException.class, () -> game.resolve(2, 1), "not Bo's turn");
        game.resolve(1, 2);
        game.draw(2);
        assertThrows(ApiException.class, () -> game.resolve(2, 1), "Me doesn't take a target");
    }
}
