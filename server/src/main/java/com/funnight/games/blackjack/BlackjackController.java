package com.funnight.games.blackjack;

import com.funnight.game.GameType;
import com.funnight.room.RoomService;
import com.funnight.room.RoomView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.room.RoomController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/rooms/{id}/blackjack")
public class BlackjackController {

    public record BetRequest(@NotNull Integer playerId, @NotNull Integer sips) {
    }

    public record ActionRequest(@NotNull Integer playerId, @NotNull BlackjackGame.Action action) {
    }

    private final RoomService rooms;

    public BlackjackController(RoomService rooms) {
        this.rooms = rooms;
    }

    /** A player locks in their bet (in sips) for the next deal. */
    @PostMapping("/bet")
    public RoomView bet(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                        @Valid @RequestBody BetRequest request) {
        return rooms.play(id, token, GameType.BLACKJACK, BlackjackGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.bet(request.playerId(), request.sips());
        });
    }

    /** The host deals; seats that haven't locked in a bet keep their previous one. */
    @PostMapping("/deal")
    public RoomView deal(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.play(id, token, GameType.BLACKJACK, BlackjackGame.class, (game, actor) -> {
            actor.requireHost();
            game.deal();
        });
    }

    /** Hit, stand or double down for the player whose turn it is. */
    @PostMapping("/action")
    public RoomView act(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                        @Valid @RequestBody ActionRequest request) {
        return rooms.play(id, token, GameType.BLACKJACK, BlackjackGame.class, (game, actor) -> {
            actor.requireFor(request.playerId());
            game.act(request.playerId(), request.action());
        });
    }
}
