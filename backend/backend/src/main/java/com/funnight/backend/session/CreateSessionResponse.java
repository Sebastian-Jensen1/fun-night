package com.funnight.backend.session;

/** The token is only ever returned here; clients must send it back as the X-Session-Token header. */
public record CreateSessionResponse(SessionView session, String token) {
}
