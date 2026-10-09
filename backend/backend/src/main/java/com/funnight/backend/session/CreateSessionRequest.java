package com.funnight.backend.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param mode        FULL for the six-game session, QUICKPLAY for a single game
 * @param gameId      required for QUICKPLAY: which game to jump into
 * @param playerNames one name per player, in seating order
 */
public record CreateSessionRequest(
        @NotNull SessionMode mode,
        @Size(max = 32) String gameId,
        @NotNull @Size(min = SessionService.MIN_PLAYERS, max = SessionService.MAX_PLAYERS)
        List<@NotBlank @Size(max = SessionService.MAX_NAME_LENGTH) String> playerNames
) {
}
