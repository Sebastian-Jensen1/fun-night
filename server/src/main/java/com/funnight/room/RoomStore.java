package com.funnight.room;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Where live rooms are kept. */
public interface RoomStore {
    void save(Room room);
    Optional<Room> findById(String id);
    Optional<Room> findByCode(String code);
    void delete(String id);
    int count();
    /** Removes rooms with no activity since {@code cutoff} and returns their ids. */
    List<String> removeIdleSince(Instant cutoff);
}
