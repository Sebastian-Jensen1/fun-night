package com.funnight.game;

import com.funnight.room.Actor;

import java.io.Serializable;

/**
 * The state of one game being played in a room. Implementations are not thread-safe: every
 * call happens while the room's lock is held. They are serializable so a room survives a restart.
 */
public interface Game extends Serializable {

    /** What {@code viewer} is allowed to see. Hidden information must never be included. */
    Object view(Actor viewer);

    Progress progress();
}
