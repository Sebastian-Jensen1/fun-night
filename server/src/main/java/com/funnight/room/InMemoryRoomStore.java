package com.funnight.room;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRoomStore implements RoomStore {
    private final Map<String, Room> byId = new ConcurrentHashMap<>();
    private final Map<String, String> idByCode = new ConcurrentHashMap<>();

    @Override
    public void save(Room room) {
        byId.put(room.getId(), room);
        idByCode.put(room.getCode(), room.getId());
    }

    @Override
    public Optional<Room> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Room> findByCode(String code) {
        return Optional.ofNullable(idByCode.get(code)).map(byId::get);
    }

    @Override
    public void delete(String id) {
        Room removed = byId.remove(id);
        if (removed != null) idByCode.remove(removed.getCode(), id);
    }

    @Override
    public int count() {
        return byId.size();
    }

    @Override
    public List<String> removeIdleSince(Instant cutoff) {
        List<String> removed = new ArrayList<>();
        for (Room room : List.copyOf(byId.values())) {
            if (room.getLastActivity().isBefore(cutoff)) {
                delete(room.getId());
                removed.add(room.getId());
            }
        }
        return removed;
    }
}
