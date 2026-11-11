package com.funnight.games.dares;

import com.funnight.game.GameType;
import com.funnight.room.RoomService;
import com.funnight.room.RoomView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.room.RoomController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/rooms/{id}/dares")
public class DareController {

    public record DrawRequest(@NotNull Integer playerId, @NotNull DareGame.Level level) {
    }

    public record ResolveRequest(@NotNull Integer playerId, @NotNull Boolean completed) {
    }

    private final RoomService rooms;

    public DareController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping("/draw")
    public RoomView draw(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                         @Valid @RequestBody DrawRequest request) {
        return rooms.play(id, token, GameType.DARES, DareGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.draw(request.playerId(), request.level());
        });
    }

    /** The current player either did the dare or chickened out. */
    @PostMapping("/resolve")
    public RoomView resolve(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                            @Valid @RequestBody ResolveRequest request) {
        return rooms.play(id, token, GameType.DARES, DareGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.resolve(request.playerId(), request.completed());
        });
    }
}
