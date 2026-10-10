package com.funnight.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidSessionTokenException extends ApiException {
    public InvalidSessionTokenException() {
        super(HttpStatus.FORBIDDEN, "Missing or invalid session token.");
    }
}
