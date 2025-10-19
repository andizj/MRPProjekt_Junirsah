package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.MediaRepository;
import at.technikum_wien.mrp.dao.MediaRepositoryIF;
import at.technikum_wien.mrp.model.MediaEntry;

import java.util.List;
import java.util.Optional;

public class MediaService {

    private final MediaRepositoryIF mediaRepo;

    public MediaService(MediaRepositoryIF mediaRepo) {
        this.mediaRepo = mediaRepo;
    }

    public MediaEntry create(MediaEntry entry) {
        if (entry.getTitle() == null || entry.getTitle().isBlank()) {
            throw new IllegalArgumentException("title required");
        }
        return mediaRepo.save(entry);
    }

    public Optional<MediaEntry> getById(int id) {
        return mediaRepo.findById(id);
    }

    public List<MediaEntry> getAll() {
        return mediaRepo.findAll();
    }

    public void update(MediaEntry entry, int requesterId) {
        Optional<MediaEntry> optionalExisting = mediaRepo.findById(entry.getId());
        if (optionalExisting.isEmpty()) {
            throw new IllegalArgumentException("not found");
        }
        MediaEntry existing = optionalExisting.get();
        if (existing.getCreatorId() != requesterId) {
            throw new SecurityException("not your media");
        }
        mediaRepo.save(entry);
    }

    public void delete(int id, int requesterId) {
        Optional<MediaEntry> optionalExisting = mediaRepo.findById(id);
        if (optionalExisting.isEmpty()) {
            throw new IllegalArgumentException("not found");
        }
        MediaEntry existing = optionalExisting.get();
        if (existing.getCreatorId() != requesterId) {
            throw new SecurityException("not your media");
        }
        mediaRepo.delete(id);
    }
}
