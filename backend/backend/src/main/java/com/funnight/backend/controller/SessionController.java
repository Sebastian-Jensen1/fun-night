package com.funnight.backend.controller;

import com.funnight.backend.model.Player;
import com.funnight.backend.model.Round;
import com.funnight.backend.model.Session;
import com.funnight.backend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;


@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    @Autowired
    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** Endpoint for creating a new session.*/
    @PostMapping("/create")
    public Session createSession() {
        return sessionService.createSession();
    }

    /** Endpoint for retrieving details of a specific session.*/
    @GetMapping("/{id}")
    public ResponseEntity<Session> getSession(@PathVariable Long id) {
        return sessionService.getSession(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Endpoint for adding a player to a session.*/
    @PostMapping("/{id}/player")
    public ResponseEntity<Player> addPlayer(@PathVariable Long id, @RequestBody String username) {
        try {
            String cleanUsername = username.replace("\"", "");
            Player player = sessionService.addPlayerToSession(id, cleanUsername);
            return ResponseEntity.ok(player);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    /** Endpoint for removing a player from a session.*/
    @DeleteMapping("/{id}/player/{username}")
    public ResponseEntity<Void> removePlayer(@PathVariable Long id, @PathVariable String username) {
        try {
            sessionService.removePlayerFromSession(id, username);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    /** Update username */
    @PutMapping("/{id}/player/{oldUsername}/{newUsername}")
        public ResponseEntity<Void> updateUsername(@PathVariable Long id, @PathVariable String oldUsername, @PathVariable String newUsername) {
        try {
            sessionService.editUsername(id,oldUsername,newUsername);
            return ResponseEntity.ok().build();
        }  catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

       /** Endpoint for starting a new round*/
    @PostMapping("/{id}/round")
    public ResponseEntity<Round> addRound(@PathVariable Long id) {
        try {
             Round round = sessionService.startNewRound(id);
            return ResponseEntity.ok(round);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Endpoint for listing all sessions (useful for debugging).*/
    @GetMapping("/all")
    public Collection<Session> getAllSessions() {
        return sessionService.getAllSessions().values();
    }
}


