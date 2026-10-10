package com.funnight.backend.blackjack;

import com.funnight.backend.cards.Card;
import com.funnight.backend.cards.Rank;
import com.funnight.backend.cards.Suit;
import com.funnight.backend.cards.Shoe;
import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.exception.InvalidRequestException;
import com.funnight.backend.session.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlackjackGameTest {
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player(1, "Alice");
        bob = new Player(2, "Bob");
    }

    /** Cards are dealt one to each player, then the dealer, twice; then in draw order. */
    private static BlackjackGame gameWith(Rank... order) {
        List<Card> cards = Arrays.stream(order).map(r -> new Card(r, Suit.HEARTS)).toList();
        return new BlackjackGame(Shoe.stacked(cards));
    }

    @Test
    void playerWinsHandsOutTheirBet() {
        // Alice: 10+9, Dealer: 10+7
        BlackjackGame game = gameWith(Rank.TEN, Rank.TEN, Rank.NINE, Rank.SEVEN);
        game.deal(List.of(alice), Map.of(1, 3));
        game.act(1, BlackjackGame.Action.STAND);

        assertEquals(BlackjackGame.Phase.ROUND_OVER, game.getPhase());
        assertEquals(Seat.Outcome.WIN, game.getSeats().get(0).getOutcome());
        assertEquals(3, alice.getSipsGiven());
        assertEquals(0, alice.getSipsDrunk());
    }

    @Test
    void bustingDrinksTheBet() {
        // Alice: 10+6, Dealer: 10+5, Alice hits a king and busts; dealer then draws a 3
        BlackjackGame game = gameWith(Rank.TEN, Rank.TEN, Rank.SIX, Rank.FIVE, Rank.KING, Rank.THREE);
        game.deal(List.of(alice), Map.of(1, 2));
        game.act(1, BlackjackGame.Action.HIT);

        assertEquals(Seat.Outcome.BUST, game.getSeats().get(0).getOutcome());
        assertEquals(2, alice.getSipsDrunk());
        // The dealer still plays out his hand to 17 even though everyone busted.
        assertEquals(3, game.getDealer().size());
        assertEquals(18, game.getDealer().value());
    }

    @Test
    void naturalBlackjackPaysThreeToTwoRoundedUp() {
        // Alice: A+K, Bob: 9+10, Dealer: 10+8
        BlackjackGame game = gameWith(Rank.ACE, Rank.NINE, Rank.TEN, Rank.KING, Rank.TEN, Rank.EIGHT);
        game.deal(List.of(alice, bob), Map.of(1, 3, 2, 1));

        assertEquals(bob.getId(), game.getCurrentSeat().getPlayer().getId(), "Alice has blackjack so Bob acts first");
        game.act(2, BlackjackGame.Action.STAND);

        assertEquals(5, alice.getSipsGiven());
        assertEquals(Seat.Outcome.WIN, game.getSeats().get(1).getOutcome());
    }

    @Test
    void dealerBlackjackEndsRoundImmediately() {
        // Alice: 10+9, Dealer: A+K
        BlackjackGame game = gameWith(Rank.TEN, Rank.ACE, Rank.NINE, Rank.KING);
        game.deal(List.of(alice), Map.of(1, 4));

        assertEquals(BlackjackGame.Phase.ROUND_OVER, game.getPhase());
        assertEquals(Seat.Outcome.LOSE, game.getSeats().get(0).getOutcome());
        assertEquals(4, alice.getSipsDrunk());
    }

    @Test
    void dealerDrawsToSeventeenAndCanBust() {
        // Alice: 10+8, Dealer: 10+6 then draws 9 -> 25
        BlackjackGame game = gameWith(Rank.TEN, Rank.TEN, Rank.EIGHT, Rank.SIX, Rank.NINE);
        game.deal(List.of(alice), Map.of(1, 1));
        game.act(1, BlackjackGame.Action.STAND);

        assertTrue(game.getDealer().isBust());
        assertEquals(Seat.Outcome.WIN, game.getSeats().get(0).getOutcome());
    }

    @Test
    void doubleDownDoublesTheStakes() {
        // Alice: 6+5, Dealer: 10+8, Alice doubles into a 10 -> 21
        BlackjackGame game = gameWith(Rank.SIX, Rank.TEN, Rank.FIVE, Rank.EIGHT, Rank.TEN);
        game.deal(List.of(alice), Map.of(1, 2));
        game.act(1, BlackjackGame.Action.DOUBLE);

        Seat seat = game.getSeats().get(0);
        assertTrue(seat.isDoubled());
        assertEquals(4, seat.getBet());
        assertEquals(4, alice.getSipsGiven());
    }

    @Test
    void pushCostsNothing() {
        BlackjackGame game = gameWith(Rank.TEN, Rank.TEN, Rank.EIGHT, Rank.EIGHT);
        game.deal(List.of(alice), Map.of(1, 5));
        game.act(1, BlackjackGame.Action.STAND);

        assertEquals(Seat.Outcome.PUSH, game.getSeats().get(0).getOutcome());
        assertEquals(0, alice.getSipsDrunk());
        assertEquals(0, alice.getSipsGiven());
    }

    @Test
    void cannotActOutOfTurn() {
        BlackjackGame game = gameWith(Rank.TEN, Rank.TEN, Rank.TEN, Rank.SEVEN, Rank.SEVEN, Rank.EIGHT);
        game.deal(List.of(alice, bob), Map.of(1, 1, 2, 1));
        assertThrows(GameRuleException.class, () -> game.act(2, BlackjackGame.Action.HIT));
    }

    @Test
    void betsMustBeInRangeAndCoverEveryPlayer() {
        BlackjackGame game = new BlackjackGame(new Shoe());
        assertThrows(InvalidRequestException.class, () -> game.deal(List.of(alice), Map.of(1, 0)));
        assertThrows(InvalidRequestException.class, () -> game.deal(List.of(alice), Map.of(1, 6)));
        assertThrows(InvalidRequestException.class, () -> game.deal(List.of(alice, bob), Map.of(1, 2)));
        assertThrows(InvalidRequestException.class, () -> game.deal(List.of(alice), Map.of(1, 2, 99, 2)));
    }

    @Test
    void holeCardIsNotSentWhilePlayersAreActing() {
        BlackjackGame game = gameWith(Rank.TEN, Rank.TEN, Rank.NINE, Rank.SEVEN);
        game.deal(List.of(alice), Map.of(1, 1));

        BlackjackView view = BlackjackView.of(game);
        assertEquals(1, view.dealer().cards().size());
        assertEquals(1, view.dealer().hiddenCards());
        assertEquals(10, view.dealer().value());

        game.act(1, BlackjackGame.Action.STAND);
        assertEquals(2, BlackjackView.of(game).dealer().cards().size());
    }
}
