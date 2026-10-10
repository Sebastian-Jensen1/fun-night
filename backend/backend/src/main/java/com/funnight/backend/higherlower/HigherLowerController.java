package com.funnight.backend.higherlower;

import com.funnight.backend.cards.Shoe;
import com.funnight.backend.game.GameType;
import com.funnight.backend.session.GameSession;
import com.funnight.backend.session.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;

import static com.funnight.backend.session.SessionController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/sessions/{id}/higher-lower")
public class HigherLowerController {

    public record GuessRequest(@NotNull Integer playerId, @NotNull HigherLowerGame.Guess guess) {
    }

    public record PassRequest(@NotNull Integer playerId) {
    }

    private final SessionService sessionService;

    public HigherLowerController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public HigherLowerView state(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return sessionService.withSession(id, token, session -> HigherLowerView.of(game(session)));
    }

    @PostMapping("/guess")
    public HigherLowerView guess(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                                 @Valid @RequestBody GuessRequest request) {
        return sessionService.withSession(id, token, session -> {
            HigherLowerGame game = game(session);
            game.guess(request.playerId(), request.guess());
            return HigherLowerView.of(game);
        });
    }

    /** Pass the pile on after a hot streak. */
    @PostMapping("/pass")
    public HigherLowerView pass(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                                @Valid @RequestBody PassRequest request) {
        return sessionService.withSession(id, token, session -> {
            HigherLowerGame game = game(session);
            game.pass(request.playerId());
            return HigherLowerView.of(game);
        });
    }

    private static HigherLowerGame game(GameSession session) {
        return session.game(GameType.HIGHER_LOWER, HigherLowerGame.class,
                () -> new HigherLowerGame(new Shoe(1, new SecureRandom()), session.getPlayers()));
    }
}
