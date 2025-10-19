package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.MediaEntry;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaRepository implements MediaRepositoryIF {

    private final Map<Integer, MediaEntry> mediaStore = new HashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Override
    public MediaEntry save(MediaEntry entry) {
        if (entry.getId() == 0) {
            entry.setId(idCounter.getAndIncrement());
        }
        mediaStore.put(entry.getId(), entry);
        return entry;
    }

    @Override
    public Optional<MediaEntry> findById(int id) {
        return Optional.ofNullable(mediaStore.get(id));
    }

    @Override
    public List<MediaEntry> findAll() {
        return new ArrayList<>(mediaStore.values());
    }

    @Override
    public void delete(int id) {
        mediaStore.remove(id);
    }
}
