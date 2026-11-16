package com.funnight.room;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a copy of every room on disk so a server restart (a deploy, a crash, a laptop going to
 * sleep) doesn't end the night. Rooms are marked dirty when they change and written in the
 * background. Disabled when no data directory is configured.
 */
@Component
public class RoomSnapshots {
    private static final Logger log = LoggerFactory.getLogger(RoomSnapshots.class);
    private static final String SUFFIX = ".room";
    /** Only our own classes and plain JDK value/collection types may be read back from disk. */
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=64;com.funnight.**;java.util.**;java.time.**;java.lang.**;!*");

    private final Path dir;
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();

    public RoomSnapshots(@Value("${funnight.rooms.data-dir:}") String dataDir) {
        this.dir = dataDir == null || dataDir.isBlank() ? null : Path.of(dataDir.strip());
    }

    public boolean enabled() {
        return dir != null;
    }

    public void markDirty(String roomId) {
        if (enabled()) dirty.add(roomId);
    }

    /** Returns the ids changed since the last call and forgets them. */
    public List<String> drainDirty() {
        List<String> ids = new ArrayList<>(dirty);
        ids.forEach(dirty::remove);
        return ids;
    }

    public void write(Room room) {
        if (!enabled()) return;
        try {
            byte[] bytes;
            synchronized (room) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
                try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
                    out.writeObject(room);
                }
                bytes = buffer.toByteArray();
            }
            Files.createDirectories(dir);
            Path tmp = dir.resolve(room.getId() + ".tmp");
            Files.write(tmp, bytes);
            Files.move(tmp, file(room.getId()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not save room {}: {}", room.getId(), e.toString());
        }
    }

    public void delete(String roomId) {
        if (!enabled()) return;
        dirty.remove(roomId);
        try {
            Files.deleteIfExists(file(roomId));
        } catch (IOException e) {
            log.warn("Could not delete saved room {}: {}", roomId, e.toString());
        }
    }

    /** Reads every saved room. Files that can't be read (e.g. written by an older version) are dropped. */
    public List<Room> loadAll() {
        List<Room> rooms = new ArrayList<>();
        if (!enabled() || !Files.isDirectory(dir)) return rooms;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*" + SUFFIX)) {
            for (Path path : files) {
                try (ObjectInputStream in = new RoomInputStream(Files.newInputStream(path))) {
                    rooms.add((Room) in.readObject());
                } catch (IOException | ClassNotFoundException | RuntimeException e) {
                    log.warn("Dropping unreadable saved room {}: {}", path.getFileName(), e.toString());
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException e) {
            log.warn("Could not read saved rooms from {}: {}", dir, e.toString());
        }
        return rooms;
    }

    private Path file(String roomId) {
        return dir.resolve(roomId + SUFFIX);
    }

    /** Resolves classes with the application's class loader and applies the allow-list filter. */
    private static final class RoomInputStream extends ObjectInputStream {
        RoomInputStream(InputStream in) throws IOException {
            super(in);
            setObjectInputFilter(FILTER);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(desc.getName(), false, Room.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }
    }
}
