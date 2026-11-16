package com.funnight.games;

import com.funnight.cards.Card;
import com.funnight.cards.Deck;
import com.funnight.cards.Rank;
import com.funnight.common.ApiException;
import com.funnight.games.blackjack.BlackjackGame;
import com.funnight.games.blackjack.BlackjackGame.Action;
import com.funnight.games.blackjack.Hand;
import com.funnight.games.blackjack.Seat;
import com.funnight.room.Actor;
import com.funnight.room.Player;
import com.funnight.room.Room;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.funnight.TestRooms.card;
import static com.funnight.TestRooms.room;
import static org.junit.jupiter.api.Assertions.*;

class BlackjackGameTest {

    /** Cards are dealt player 1, player 2, ..., dealer, twice over, then drawn in order. */
    private static BlackjackGame game(Room room, Rank... order) {
        List<Card> cards = Arrays.stream(order).map(r -> card(r)).toList();
        return new BlackjackGame(room, Deck.stacked(cards));
    }

    @Test
    void handCountsAcesAsElevenUntilThatWouldBust() {
        Hand hand = new Hand();
        hand.add(card(Rank.ACE));
        hand.add(card(Rank.SIX));
        assertEquals(17, hand.value());
        assertTrue(hand.isSoft());
        hand.add(card(Rank.NINE));
        assertEquals(16, hand.value());
        assertFalse(hand.isSoft());
    }

    @Test
    void winnerGetsTheirBetToHandOutAndLoserDrinks() {
        Room room = room("Anna", "Bo");
        // Anna: 10+9=19, Bo: 10+6=16, dealer: 10+8=18
        BlackjackGame game = game(room, Rank.TEN, Rank.TEN, Rank.TEN, Rank.NINE, Rank.SIX, Rank.EIGHT);
        game.bet(1, 3);
        game.bet(2, 2);
        game.deal();
        game.act(1, Action.STAND);
        game.act(2, Action.STAND);

        assertEquals(BlackjackGame.Phase.ROUND_OVER, game.getPhase());
        Player anna = room.player(1);
        Player bo = room.player(2);
        assertEquals(Seat.Outcome.WIN, game.seatOf(1).getOutcome());
        assertEquals(3, anna.getPendingGive());
        assertEquals(0, anna.getSipsGiven(), "sips only count as given once somebody drinks them");
        assertEquals(Seat.Outcome.LOSE, game.seatOf(2).getOutcome());
        assertEquals(2, bo.getSipsDrunk());
    }

    @Test
    void naturalBlackjackPaysThreeToTwoRoundedUp() {
        Room room = room("Anna", "Bo");
        // Anna: A+K, Bo: 10+7, dealer: 10+7
        BlackjackGame game = game(room, Rank.ACE, Rank.TEN, Rank.TEN, Rank.KING, Rank.SEVEN, Rank.SEVEN);
        game.bet(1, 3);
        game.deal();
        assertEquals(Seat.Status.BLACKJACK, game.seatOf(1).getStatus());
        game.act(2, Action.STAND);
        assertEquals(5, room.player(1).getPendingGive());
        assertEquals(Seat.Outcome.PUSH, game.seatOf(2).getOutcome());
    }

    @Test
    void dealerBlackjackEndsTheRoundBeforeAnyoneActs() {
        Room room = room("Anna");
        BlackjackGame game = game(room, Rank.TEN, Rank.ACE, Rank.NINE, Rank.KING);
        game.deal();
        assertEquals(BlackjackGame.Phase.ROUND_OVER, game.getPhase());
        assertEquals(1, room.player(1).getSipsDrunk());
    }

    @Test
    void doubleDownDoublesTheStakeAndTakesExactlyOneCard() {
        Room room = room("Anna");
        // Anna: 5+6, dealer: 10+7, Anna doubles and draws 10 => 21
        BlackjackGame game = game(room, Rank.FIVE, Rank.TEN, Rank.SIX, Rank.SEVEN, Rank.TEN);
        game.bet(1, 2);
        game.deal();
        game.act(1, Action.DOUBLE);
        assertEquals(BlackjackGame.Phase.ROUND_OVER, game.getPhase());
        assertEquals(4, game.seatOf(1).getStake());
        assertEquals(2, game.seatOf(1).getBet(), "the doubled stake doesn't carry over to the next round");
        // Solo there is nobody to hand sips to, so they count as given straight away
        assertEquals(4, room.player(1).getSipsGiven());
    }

    @Test
    void bustingDrinksAndTheDealerDoesNotDrawWhenNobodyIsStanding() {
        Room room = room("Anna");
        BlackjackGame game = game(room, Rank.TEN, Rank.TEN, Rank.SIX, Rank.SIX, Rank.KING, Rank.FIVE);
        game.deal();
        game.act(1, Action.HIT);
        assertEquals(Seat.Outcome.BUST, game.seatOf(1).getOutcome());
        assertEquals(2, game.getDealer().size());
    }

    @Test
    void enforcesTurnsAndBetLimits() {
        Room room = room("Anna", "Bo");
        BlackjackGame game = game(room, Rank.TWO, Rank.THREE, Rank.TEN, Rank.FOUR, Rank.FIVE, Rank.SEVEN);
        assertThrows(ApiException.class, () -> game.bet(1, 0));
        assertThrows(ApiException.class, () -> game.bet(1, 6));
        assertThrows(ApiException.class, () -> game.act(1, Action.HIT), "no hand in play yet");
        game.deal();
        assertThrows(ApiException.class, () -> game.act(2, Action.HIT), "it's Anna's turn");
        assertThrows(ApiException.class, () -> game.bet(1, 2), "bets are closed mid-round");
        assertThrows(ApiException.class, game::deal);
    }

    @Test
    void holeCardIsNeverSentWhileHandsAreInPlay() {
        Room room = room("Anna");
        BlackjackGame game = game(room, Rank.TEN, Rank.NINE, Rank.SIX, Rank.KING);
        game.deal();
        BlackjackGame.View view = game.view(Actor.ofHost());
        assertEquals(1, view.dealer().cards().size());
        assertEquals(1, view.dealer().hiddenCards());
        assertEquals("9", view.dealer().cards().get(0).rank());
    }
}
