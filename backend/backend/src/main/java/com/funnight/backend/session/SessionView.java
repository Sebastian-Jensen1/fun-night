package com.funnight.backend.session;

import com.funnight.backend.game.GameInfo;

import java.util.List;

/** What the client sees of a session. Never includes the token or its hash. */
public record SessionView(
        String id,
        SessionMode mode,
        List<PlayerView> players,
        List<SlotView> games,
        int currentGameIndex,
        GameInfo currentGame,
        boolean finished
) {
    public record PlayerView(int id, String name, int sipsDrunk, int sipsGiven) {
        static PlayerView of(Player p) {
            return new PlayerView(p.getId(), p.getName(), p.getSipsDrunk(), p.getSipsGiven());
        }
    }

    public record SlotView(String id, String name, boolean available, GameSlot.Status status) {
        static SlotView of(GameSlot slot) {
            return new SlotView(slot.getType().getId(), slot.getType().getDisplayName(),
                    slot.getType().isAvailable(), slot.getStatus());
        }
    }

    public static SessionView of(GameSession s) {
        return new SessionView(
                s.getId(),
                s.getMode(),
                s.getPlayers().stream().map(PlayerView::of).toList(),
                s.getSlots().stream().map(SlotView::of).toList(),
                s.getCurrentIndex(),
                s.currentGame().map(GameInfo::of).orElse(null),
                s.isFinished());
    }
}
