package com.funnight.backend.exception;

public class EmptySessionException extends RuntimeException {
    public EmptySessionException(Long sessionId, Long roundId) {
        super("session" + sessionId + "with round" + roundId + " is empty");
    }
}
