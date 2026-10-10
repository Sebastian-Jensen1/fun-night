package com.funnight.backend.dares;

import com.funnight.backend.exception.GameRuleException;
import com.funnight.backend.model.Challenge;
import com.funnight.backend.session.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DareGameTest {

    @Test
    void chickeningOutCostsSipsAndPassesTheTurn() {
        Player a = new Player(1, "A");
        Player b = new Player(2, "B");
        DareGame game = new DareGame(List.of(new Challenge("Dance")), List.of(a, b));

        game.draw(1);
        assertEquals("Dance", game.getCurrentDare());
        game.resolve(1, false);

        assertEquals(DareGame.CHICKEN_SIPS, a.getSipsDrunk());
        assertEquals(2, game.getCurrentPlayer().getId());

        game.draw(2);
        game.resolve(2, true);
        assertEquals(DareGame.COMPLETED_SIPS_TO_GIVE, b.getSipsGiven());
        assertEquals(1, game.getCurrentPlayer().getId());
    }

    @Test
    void enforcesTurnOrderAndPhase() {
        DareGame game = new DareGame(List.of(new Challenge("Dance")), List.of(new Player(1, "A"), new Player(2, "B")));
        assertThrows(GameRuleException.class, () -> game.draw(2));
        assertThrows(GameRuleException.class, () -> game.resolve(1, true));
    }
}
