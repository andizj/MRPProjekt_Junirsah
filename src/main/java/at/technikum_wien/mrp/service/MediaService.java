package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.FavoriteRepositoryIF;
import at.technikum_wien.mrp.dao.MediaRepository;
import at.technikum_wien.mrp.dao.MediaRepositoryIF;
import at.technikum_wien.mrp.model.MediaEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MediaService {

    private final MediaRepositoryIF mediaRepo;
    private final FavoriteRepositoryIF favoriteRepo;

    public MediaService(MediaRepositoryIF mediaRepo, FavoriteRepositoryIF favoriteRepo) {
        this.mediaRepo = mediaRepo;
        this.favoriteRepo = favoriteRepo;
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

    public List<MediaEntry> getFiltered(String search, String type, String genre, Integer year, Integer minAge, String sortBy){
        return mediaRepo.findAll(search, type, genre, year, minAge, sortBy);
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
    public void addFavorite(int mediaId, int userId) {
        if (mediaRepo.findById(mediaId).isEmpty()) {
            throw new IllegalArgumentException("media not found");
        }
        favoriteRepo.addFavorite(userId, mediaId);
    }
    public void removeFavorite(int mediaId, int userId) {
        favoriteRepo.removeFavorite(userId, mediaId);
    }
    public List<MediaEntry> getFavorites(int userId) {
        List<Integer> mediaIds = favoriteRepo.findFavoritesByUserId(userId);

        List<MediaEntry> favorites = new ArrayList<>();
        for (int id : mediaIds) {
            mediaRepo.findById(id).ifPresent(favorites::add);
        }
        return favorites;
    }
}
