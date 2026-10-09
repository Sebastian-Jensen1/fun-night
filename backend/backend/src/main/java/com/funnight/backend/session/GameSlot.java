package com.funnight.backend.session;

import com.funnight.backend.game.GameType;

/** One game in a session's line-up and how far the group has got with it. */
public class GameSlot {
    public enum Status { UPCOMING, ACTIVE, COMPLETED, SKIPPED }

    private final GameType type;
    private Status status = Status.UPCOMING;

    public GameSlot(GameType type) {
        this.type = type;
    }

    public GameType getType() { return type; }
    public Status getStatus() { return status; }
    void setStatus(Status status) { this.status = status; }
}
