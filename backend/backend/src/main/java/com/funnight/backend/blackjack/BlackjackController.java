package com.funnight.backend.blackjack;

import com.funnight.backend.session.GameSession;
import com.funnight.backend.session.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.funnight.backend.session.SessionController.TOKEN_HEADER;

@RestController
@RequestMapping("/api/sessions/{id}/blackjack")
public class BlackjackController {

    public record DealRequest(@NotEmpty @Size(max = 8) Map<@NotNull Integer, @NotNull Integer> bets) {
    }

    public record ActionRequest(@NotNull Integer playerId, @NotNull BlackjackGame.Action action) {
    }

    private final SessionService sessionService;

    public BlackjackController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public BlackjackView table(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return sessionService.withSession(id, token, session -> BlackjackView.of(game(session)));
    }

    /** Places everyone's bets (in sips) and deals a new round. */
    @PostMapping("/deal")
    public BlackjackView deal(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                              @Valid @RequestBody DealRequest request) {
        return sessionService.withSession(id, token, session -> {
            BlackjackGame game = game(session);
            game.deal(session.getPlayers(), request.bets());
            return BlackjackView.of(game);
        });
    }

    /** Hit, stand or double down for the player whose turn it is. */
    @PostMapping("/action")
    public BlackjackView act(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                             @Valid @RequestBody ActionRequest request) {
        return sessionService.withSession(id, token, session -> {
            BlackjackGame game = game(session);
            game.act(request.playerId(), request.action());
            return BlackjackView.of(game);
        });
    }

    private static BlackjackGame game(GameSession session) {
        return session.blackjack(() -> new BlackjackGame(new Shoe()));
    }
}
