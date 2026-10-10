package com.funnight.backend.exception;

import org.springframework.http.HttpStatus;

public class CapacityExceededException extends ApiException {
    public CapacityExceededException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The server is full right now. Please try again in a little while.");
    }
}
