package com.funnight.games.higherlower;

import com.funnight.game.GameType;
import com.funnight.room.RoomService;
import com.funnight.room.RoomView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.room.RoomController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/rooms/{id}/higher-lower")
public class HigherLowerController {

    public record GuessRequest(@NotNull Integer playerId, @NotNull HigherLowerGame.Guess guess) {
    }

    public record PassRequest(@NotNull Integer playerId) {
    }

    private final RoomService rooms;

    public HigherLowerController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping("/guess")
    public RoomView guess(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                          @Valid @RequestBody GuessRequest request) {
        return rooms.play(id, token, GameType.HIGHER_LOWER, HigherLowerGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.guess(request.playerId(), request.guess());
        });
    }

    /** Pass the pile on after a hot streak. */
    @PostMapping("/pass")
    public RoomView pass(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                         @Valid @RequestBody PassRequest request) {
        return rooms.play(id, token, GameType.HIGHER_LOWER, HigherLowerGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.pass(request.playerId());
        });
    }
}
