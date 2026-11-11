package com.funnight.games.derby;

import com.funnight.cards.Suit;
import com.funnight.game.GameType;
import com.funnight.room.RoomService;
import com.funnight.room.RoomView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.room.RoomController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/rooms/{id}/derby")
public class DerbyController {

    public record BetRequest(@NotNull Integer playerId, @NotNull Suit suit, @NotNull Integer sips) {
    }

    private final RoomService rooms;

    public DerbyController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping("/bet")
    public RoomView bet(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                        @Valid @RequestBody BetRequest request) {
        return rooms.play(id, token, GameType.DERBY, DerbyGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.bet(request.playerId(), request.suit(), request.sips());
        });
    }

    @PostMapping("/start")
    public RoomView start(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.play(id, token, GameType.DERBY, DerbyGame.class, (game, actor) -> {
            actor.requireHost();
            game.start();
        });
    }

    /** Anyone at the table may turn the next card; the host screen does it automatically. */
    @PostMapping("/flip")
    public RoomView flip(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.play(id, token, GameType.DERBY, DerbyGame.class, (game, actor) -> game.flip());
    }

    @PostMapping("/again")
    public RoomView again(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.play(id, token, GameType.DERBY, DerbyGame.class, (game, actor) -> {
            actor.requireHost();
            game.newRace();
        });
    }
}
