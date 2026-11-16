package com.funnight.room;

import com.funnight.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live updates. Every screen in a room keeps one server-sent-events stream open; whenever the
 * room changes each stream gets a tiny "something changed" ping and the client fetches its own
 * view of the room. Nothing secret ever travels over the stream itself.
 */
@Component
public class RoomEvents {
    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final int MAX_STREAMS_PER_ROOM = 32;

    private record Stream(SseEmitter emitter, Integer playerId) {
    }

    private final Map<String, List<Stream>> streams = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String roomId, Actor actor, long version) {
        List<Stream> list = streams.computeIfAbsent(roomId, id -> new CopyOnWriteArrayList<>());
        if (list.size() >= MAX_STREAMS_PER_ROOM) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many screens are connected to this game night.");
        }
        Stream stream = new Stream(new SseEmitter(STREAM_TIMEOUT_MS), actor.playerId());
        Runnable remove = () -> {
            // Others see this phone go offline
            if (list.remove(stream) && stream.playerId() != null) publish(roomId, "presence");
        };
        stream.emitter().onCompletion(remove);
        stream.emitter().onError(error -> remove.run());
        stream.emitter().onTimeout(stream.emitter()::complete);
        list.add(stream);
        send(list, stream, String.valueOf(version));
        if (stream.playerId() != null) publish(roomId, "presence");
        return stream.emitter();
    }

    /** Tells every screen in the room to refresh. */
    public void publish(String roomId, String data) {
        List<Stream> list = streams.get(roomId);
        if (list == null) return;
        for (Stream stream : list) send(list, stream, data);
    }

    public boolean isOnline(String roomId, int playerId) {
        List<Stream> list = streams.get(roomId);
        return list != null && list.stream().anyMatch(s -> Objects.equals(s.playerId(), playerId));
    }

    /** Ends the streams of a player whose seat was released or removed. */
    public void dropPlayer(String roomId, int playerId) {
        List<Stream> list = streams.get(roomId);
        if (list == null) return;
        for (Stream stream : list) {
            if (Objects.equals(stream.playerId(), playerId)) {
                list.remove(stream);
                stream.emitter().complete();
            }
        }
    }

    /** Ends every stream of a room that no longer exists. */
    public void close(String roomId) {
        List<Stream> list = streams.remove(roomId);
        if (list == null) return;
        for (Stream stream : list) {
            try {
                stream.emitter().send(SseEmitter.event().name("closed").data("closed"));
            } catch (IOException | RuntimeException ignored) {
                // the client is already gone
            }
            stream.emitter().complete();
        }
    }

    /** Keeps idle streams alive through proxies and notices phones that silently disappeared. */
    @Scheduled(fixedDelay = 20_000)
    public void heartbeat() {
        streams.forEach((roomId, list) -> {
            for (Stream stream : list) {
                try {
                    stream.emitter().send(SseEmitter.event().comment("hb"));
                } catch (IOException | RuntimeException e) {
                    drop(roomId, list, stream);
                }
            }
        });
    }

    private void send(List<Stream> list, Stream stream, String data) {
        try {
            stream.emitter().send(SseEmitter.event().name("room").data(data));
        } catch (IOException | RuntimeException e) {
            list.remove(stream);
        }
    }

    private void drop(String roomId, List<Stream> list, Stream stream) {
        if (list.remove(stream) && stream.playerId() != null) publish(roomId, "presence");
    }
}
