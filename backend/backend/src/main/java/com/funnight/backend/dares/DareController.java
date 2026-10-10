package com.funnight.backend.dares;

import com.funnight.backend.game.GameType;
import com.funnight.backend.service.ChallengeService;
import com.funnight.backend.session.GameSession;
import com.funnight.backend.session.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import static com.funnight.backend.session.SessionController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/sessions/{id}/dares")
public class DareController {

    public record DrawRequest(@NotNull Integer playerId) {
    }

    public record ResolveRequest(@NotNull Integer playerId, @NotNull Boolean completed) {
    }

    private final SessionService sessionService;
    private final ChallengeService challengeService;

    public DareController(SessionService sessionService, ChallengeService challengeService) {
        this.sessionService = sessionService;
        this.challengeService = challengeService;
    }

    @GetMapping
    public DareView state(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return sessionService.withSession(id, token, session -> DareView.of(game(session)));
    }

    @PostMapping("/draw")
    public DareView draw(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                         @Valid @RequestBody DrawRequest request) {
        return sessionService.withSession(id, token, session -> {
            DareGame game = game(session);
            game.draw(request.playerId());
            return DareView.of(game);
        });
    }

    /** The current player either did the dare or chickened out. */
    @PostMapping("/resolve")
    public DareView resolve(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                            @Valid @RequestBody ResolveRequest request) {
        return sessionService.withSession(id, token, session -> {
            DareGame game = game(session);
            game.resolve(request.playerId(), request.completed());
            return DareView.of(game);
        });
    }

    private DareGame game(GameSession session) {
        return session.game(GameType.DARES, DareGame.class,
                () -> new DareGame(challengeService.getAllChallenges(), session.getPlayers()));
    }
}
