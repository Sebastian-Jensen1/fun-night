package com.funnight.backend.session;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    public static final String TOKEN_HEADER = "X-Session-Token";

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** Starts a full six-game session or a quickplay session for one game. */
    @PostMapping
    public ResponseEntity<CreateSessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(request));
    }

    @GetMapping("/{id}")
    public SessionView get(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return sessionService.get(id, token);
    }

    /** Marks the current game as done and moves on to the next one. */
    @PostMapping("/{id}/next")
    public SessionView next(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return sessionService.advance(id, token);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> end(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        sessionService.end(id, token);
        return ResponseEntity.noContent().build();
    }
}
