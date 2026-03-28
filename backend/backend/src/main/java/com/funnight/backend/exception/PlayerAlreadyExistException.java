package com.funnight.backend.exception;

public class PlayerAlreadyExistException extends RuntimeException {
    public PlayerAlreadyExistException(String username, Long sessionId) {
        super("Player with username " + username + " already exists in session" + sessionId);}
}
