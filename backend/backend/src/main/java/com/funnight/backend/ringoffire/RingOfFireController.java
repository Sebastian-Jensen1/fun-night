package com.funnight.backend.ringoffire;

import com.funnight.backend.game.GameType;
import com.funnight.backend.session.GameSession;
import com.funnight.backend.session.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.backend.session.SessionController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/sessions/{id}/ring-of-fire")
public class RingOfFireController {

    public record DrawRequest(@NotNull Integer playerId) {
    }

    /** {@code targetId} is who drinks (or the new mate) for cards that need someone picked. */
    public record ResolveRequest(@NotNull Integer playerId, Integer targetId) {
    }

    private final SessionService sessionService;

    public RingOfFireController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public RingOfFireView state(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return sessionService.withSession(id, token, session -> RingOfFireView.of(game(session)));
    }

    @PostMapping("/draw")
    public RingOfFireView draw(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                               @Valid @RequestBody DrawRequest request) {
        return sessionService.withSession(id, token, session -> {
            RingOfFireGame game = game(session);
            game.draw(request.playerId());
            return RingOfFireView.of(game);
        });
    }

    @PostMapping("/resolve")
    public RingOfFireView resolve(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                                  @Valid @RequestBody ResolveRequest request) {
        return sessionService.withSession(id, token, session -> {
            RingOfFireGame game = game(session);
            game.resolve(request.playerId(), request.targetId());
            return RingOfFireView.of(game);
        });
    }

    /** Lay out a fresh ring once the 4th king has been pulled. */
    @PostMapping("/restart")
    public RingOfFireView restart(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return sessionService.withSession(id, token, session -> {
            RingOfFireGame game = game(session);
            game.restart();
            return RingOfFireView.of(game);
        });
    }

    private static RingOfFireGame game(GameSession session) {
        return session.game(GameType.RING_OF_FIRE, RingOfFireGame.class, () -> new RingOfFireGame(session.getPlayers()));
    }
}
