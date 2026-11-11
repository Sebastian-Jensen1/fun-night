package com.funnight.game;

import com.funnight.room.Room;

/** Creates the fresh state for a game when a room reaches it. */
public interface GameFactory {
    Game create(GameType type, Room room);
}
