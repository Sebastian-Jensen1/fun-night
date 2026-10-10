package com.funnight.backend.exception;

import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApiException {
    public SessionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Session does not exist or has expired.");
    }
}
