package com.funnight.backend.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String username, Long sessionId) {
        super("Player with username " + username + " does not exist in session " + sessionId);
    }
}
