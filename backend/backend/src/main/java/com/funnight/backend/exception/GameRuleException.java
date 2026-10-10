package com.funnight.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a request is well-formed but not allowed in the current game state (e.g. hitting out of turn). */
public class GameRuleException extends ApiException {
    public GameRuleException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
