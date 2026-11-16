package com.funnight.room;

import com.funnight.game.GameInfo;
import com.funnight.game.GameType;
import com.funnight.game.Progress;

import java.util.List;

/** What one screen sees of a room. Never includes tokens, and the game part is tailored to the viewer. */
public record RoomView(
        String id,
        String code,
        Room.Mode mode,
        Room.Phase phase,
        long version,
        You you,
        List<PlayerView> players,
        List<SlotView> lineup,
        int currentIndex,
        GameInfo currentGame,
        String nextGame,
        Object game,
        Progress progress
) {
    public record You(String role, Integer playerId) {
    }

    public record PlayerView(int id, String name, int sipsDrunk, int sipsGiven, int pendingGive,
                             boolean remote, boolean online) {
    }

    public record SlotView(String id, String name, String tagline, int minPlayers, boolean playable, Slot.Status status) {
    }

    public static RoomView of(Room room, Actor viewer, RoomEvents events) {
        List<PlayerView> players = room.getPlayers().stream()
                .map(p -> new PlayerView(p.getId(), p.getName(), p.getSipsDrunk(), p.getSipsGiven(), p.getPendingGive(),
                        p.isRemote(), p.isRemote() && events.isOnline(room.getId(), p.getId())))
                .toList();
        List<SlotView> lineup = room.getSlots().stream()
                .map(s -> {
                    GameType t = s.getType();
                    return new SlotView(t.getId(), t.getDisplayName(), t.getTagline(), t.getMinPlayers(),
                            room.canPlay(t), s.getStatus());
                })
                .toList();
        boolean playing = room.getPhase() == Room.Phase.PLAYING;
        return new RoomView(
                room.getId(),
                room.getCode(),
                room.getMode(),
                room.getPhase(),
                room.getVersion(),
                new You(viewer.host() ? "HOST" : "PLAYER", viewer.playerId()),
                players,
                lineup,
                room.getCurrentIndex(),
                room.currentGame().map(GameInfo::of).orElse(null),
                playing ? room.nextGame().map(GameType::getDisplayName).orElse(null) : null,
                playing ? room.getGame().view(viewer) : null,
                playing ? room.getGame().progress() : null);
    }
}
