package com.funnight.backend.exception;

public class NoChallengesAvailableException extends RuntimeException {
    public NoChallengesAvailableException() {
        super("No challenges are available in the system. Please add some challenges first.");
    }
}
