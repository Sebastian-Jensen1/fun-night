package com.funnight.room;

import com.funnight.common.ApiException;

/**
 * Who is making a request. The host device may act for every seat, so the night never stalls
 * when a phone dies; a player's own device may only act for that player.
 */
public record Actor(boolean host, Integer playerId) {

    public static Actor ofHost() {
        return new Actor(true, null);
    }

    public static Actor ofPlayer(int playerId) {
        return new Actor(false, playerId);
    }

    public boolean canActFor(int id) {
        return host || (playerId != null && playerId == id);
    }

    public void requireFor(int id) {
        if (!canActFor(id)) throw ApiException.notAllowed("You can only play your own seat.");
    }

    public void requireHost() {
        if (!host) throw ApiException.notAllowed("Only the host can do that.");
    }
}
