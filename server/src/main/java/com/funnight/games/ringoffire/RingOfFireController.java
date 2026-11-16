package com.funnight.games.ringoffire;

import com.funnight.game.GameType;
import com.funnight.room.RoomService;
import com.funnight.room.RoomView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.room.RoomController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/rooms/{id}/ring-of-fire")
public class RingOfFireController {

    public record DrawRequest(@NotNull Integer playerId) {
    }

    /** {@code targetId} is who drinks (or the new mate) for cards that need someone picked. */
    public record ResolveRequest(@NotNull Integer playerId, Integer targetId) {
    }

    private final RoomService rooms;

    public RingOfFireController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping("/draw")
    public RoomView draw(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                         @Valid @RequestBody DrawRequest request) {
        return rooms.play(id, token, GameType.RING_OF_FIRE, RingOfFireGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.draw(request.playerId());
        });
    }

    @PostMapping("/resolve")
    public RoomView resolve(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                            @Valid @RequestBody ResolveRequest request) {
        return rooms.play(id, token, GameType.RING_OF_FIRE, RingOfFireGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.resolve(request.playerId(), request.targetId());
        });
    }

    /** Lay out a fresh ring once the 4th king has been pulled. */
    @PostMapping("/restart")
    public RoomView restart(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.play(id, token, GameType.RING_OF_FIRE, RingOfFireGame.class, (game, actor) -> {
            actor.requireHost();
            game.restart();
        });
    }
}
