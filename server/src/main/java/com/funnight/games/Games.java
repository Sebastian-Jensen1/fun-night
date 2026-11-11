package com.funnight.games;

import com.funnight.cards.Deck;
import com.funnight.content.ContentLibrary;
import com.funnight.game.Game;
import com.funnight.game.GameFactory;
import com.funnight.game.GameType;
import com.funnight.games.blackjack.BlackjackGame;
import com.funnight.games.dares.DareGame;
import com.funnight.games.derby.DerbyGame;
import com.funnight.games.guilty.GuiltyGame;
import com.funnight.games.higherlower.HigherLowerGame;
import com.funnight.games.ringoffire.RingOfFireGame;
import com.funnight.room.Room;
import org.springframework.stereotype.Component;

/** Sets up each game with a freshly shuffled deck and its content. */
@Component
public class Games implements GameFactory {
    private final ContentLibrary content;

    public Games(ContentLibrary content) {
        this.content = content;
    }

    @Override
    public Game create(GameType type, Room room) {
        return switch (type) {
            case BLACKJACK -> new BlackjackGame(room, Deck.shuffled(4));
            case DARES -> new DareGame(room, content.mildDares(), content.wildDares());
            case HIGHER_LOWER -> new HigherLowerGame(room, Deck.shuffled(1));
            case RING_OF_FIRE -> new RingOfFireGame(room, Deck.shuffled(1));
            case GUILTY -> new GuiltyGame(room, content.guiltyPrompts());
            case DERBY -> new DerbyGame(room);
        };
    }
}
