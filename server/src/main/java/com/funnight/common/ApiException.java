package com.funnight.common;

import org.springframework.http.HttpStatus;

/** An error that is safe to show to the client, mapped to an HTTP status. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** The request itself is wrong (bad name, unknown player, bet out of range, ...). */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** The request is fine but the game doesn't allow it right now (not your turn, wrong phase, ...). */
    public static ApiException rule(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException invalidToken() {
        return new ApiException(HttpStatus.FORBIDDEN, "Missing or invalid room token.");
    }

    /** The caller is known but may not do this (a player acting for someone else, host-only actions). */
    public static ApiException notAllowed(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException busy(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
