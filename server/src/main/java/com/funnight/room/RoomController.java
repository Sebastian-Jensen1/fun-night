package com.funnight.room;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    public static final String TOKEN_HEADER = "X-Room-Token";

    /**
     * @param mode        FULL for the six-game night, QUICKPLAY for a single game
     * @param gameId      required for QUICKPLAY: which game to jump into
     * @param playerNames optional seats played on the host device; phones can join afterwards
     */
    public record CreateRequest(
            @NotNull Room.Mode mode,
            @Size(max = 32) String gameId,
            @Size(max = Room.MAX_PLAYERS) List<@NotBlank @Size(max = RoomService.MAX_NAME_LENGTH) String> playerNames) {
    }

    /** Either {@code name} (a new seat, lobby only) or {@code seatId} (take over an open seat). */
    public record JoinRequest(@NotBlank @Size(max = 12) String code,
                              @Size(max = RoomService.MAX_NAME_LENGTH) String name,
                              Integer seatId) {
    }

    public record AddPlayerRequest(@NotBlank @Size(max = RoomService.MAX_NAME_LENGTH) String name) {
    }

    public record HandOutRequest(@NotNull Integer playerId, @NotNull Integer targetId, @NotNull @Min(1) @Max(50) Integer sips) {
    }

    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    /** Opens a lobby and returns the host's secret token. */
    @PostMapping
    public ResponseEntity<RoomService.Joined> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rooms.create(request.mode(), request.gameId(), request.playerNames()));
    }

    @GetMapping("/lookup")
    public RoomService.Lookup lookup(@RequestParam @Size(max = 12) String code) {
        return rooms.lookup(code);
    }

    /** A phone takes a seat and gets its own secret token. */
    @PostMapping("/join")
    public RoomService.Joined join(@Valid @RequestBody JoinRequest request) {
        return rooms.join(request.code(), request.name(), request.seatId());
    }

    @GetMapping("/{id}")
    public RoomView get(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.view(id, token);
    }

    /** Live "the room changed" pings as server-sent events. */
    @GetMapping(path = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                             HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no"); // ask nginx-style proxies not to buffer the stream
        return rooms.subscribe(id, token);
    }

    @PostMapping("/{id}/players")
    public RoomView addPlayer(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                              @Valid @RequestBody AddPlayerRequest request) {
        return rooms.addLocalPlayer(id, token, request.name());
    }

    @DeleteMapping("/{id}/players/{playerId}")
    public RoomView removePlayer(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                                 @PathVariable int playerId) {
        return rooms.removeOrRelease(id, token, playerId);
    }

    @PostMapping("/{id}/start")
    public RoomView start(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.start(id, token);
    }

    /** Marks the current game as done and moves on to the next one. */
    @PostMapping("/{id}/next")
    public RoomView next(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.next(id, token);
    }

    @PostMapping("/{id}/rematch")
    public RoomView rematch(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        return rooms.rematch(id, token);
    }

    /** A player hands sips they won to someone else. */
    @PostMapping("/{id}/handout")
    public RoomView handOut(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token,
                            @Valid @RequestBody HandOutRequest request) {
        return rooms.handOut(id, token, request.playerId(), request.targetId(), request.sips());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> end(@PathVariable String id, @RequestHeader(TOKEN_HEADER) String token) {
        rooms.end(id, token);
        return ResponseEntity.noContent().build();
    }
}
