package com.funnight.room;

import com.funnight.game.GameType;

import java.io.Serializable;

/** One game in a room's line-up and how far the group has got with it. */
public class Slot implements Serializable {
    public enum Status { UPCOMING, ACTIVE, COMPLETED, SKIPPED }

    private final GameType type;
    private Status status = Status.UPCOMING;

    Slot(GameType type) {
        this.type = type;
    }

    public GameType getType() { return type; }
    public Status getStatus() { return status; }
    void setStatus(Status status) { this.status = status; }
}
