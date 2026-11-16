package com.funnight.games.guilty;

import com.funnight.game.GameType;
import com.funnight.room.RoomService;
import com.funnight.room.RoomView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.room.RoomController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/rooms/{id}/guilty")
public class GuiltyController {

    public record VoteRequest(@NotNull Integer playerId, @NotNull Integer accusedId) {
    }

    private final RoomService rooms;

    public GuiltyController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping("/vote")
    public RoomView vote(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                         @Valid @RequestBody VoteRequest request) {
        return rooms.play(id, token, GameType.GUILTY, GuiltyGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.vote(request.playerId(), request.accusedId());
        });
    }

    /** The host reads the verdict without waiting for the last votes. */
    @PostMapping("/close")
    public RoomView close(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.play(id, token, GameType.GUILTY, GuiltyGame.class, (game, actor) -> {
            actor.requireHost();
            game.closeVoting();
        });
    }

    @PostMapping("/next")
    public RoomView next(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.play(id, token, GameType.GUILTY, GuiltyGame.class, (game, actor) -> game.nextRound());
    }
}
