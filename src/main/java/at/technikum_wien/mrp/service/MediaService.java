package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.interfaces.FavoriteRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.MediaRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.RatingRepositoryIF;
import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.Rating;

import java.util.*;

public class MediaService {

    private final MediaRepositoryIF mediaRepo;
    private final FavoriteRepositoryIF favoriteRepo;
    private final RatingRepositoryIF ratingRepo;

    public MediaService(MediaRepositoryIF mediaRepo, FavoriteRepositoryIF favoriteRepo, RatingRepositoryIF ratingRepo) {
        this.mediaRepo = mediaRepo;
        this.favoriteRepo = favoriteRepo;
        this.ratingRepo = ratingRepo;
    }

    private void validate(MediaEntry entry) {
        if (entry.getTitle() == null || entry.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title required");
        }
        if (entry.getReleaseYear() < 1800) {
            throw new IllegalArgumentException("Invalid release year (must be >= 1800)");
        }
        if (entry.getReleaseYear() > java.time.Year.now().getValue() + 10) {
            throw new IllegalArgumentException("Release year too far in the future");
        }
        if (entry.getAgeRestriction() < 0) {
            throw new IllegalArgumentException("Age restriction cannot be negative");
        }
        List<String> validTypes = List.of("MOVIE", "SERIES", "GAME");
        if (entry.getMediaType() == null || !validTypes.contains(entry.getMediaType().toUpperCase())) {
            throw new IllegalArgumentException("Invalid media type. Allowed: MOVIE, SERIES, GAME");
        }
        if (entry.getGenres() == null || entry.getGenres().length == 0) {
            throw new IllegalArgumentException("At least one genre is required");
        }
        for (String g : entry.getGenres()) {
            if (g == null || g.isBlank()) {
                throw new IllegalArgumentException("Genre cannot be empty");
            }
        }
    }

    public MediaEntry create(MediaEntry entry) {
        if (entry.getTitle() == null || entry.getTitle().isBlank()) {
            throw new IllegalArgumentException("title required");
        }
        validate(entry);
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
        validate(entry);
        entry.setCreatorId(existing.getCreatorId());

        entry.setCreatedAt(existing.getCreatedAt());
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
        if (mediaRepo.findById(mediaId).isEmpty()) {
            throw new IllegalArgumentException("media not found");
        }
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
    public List<MediaEntry> getRecommendations(int userId) {
        List<Rating> userRatings = ratingRepo.findByUserId(userId);

        Set<String> likedGenres = new HashSet<>();
        Set<Integer> knownMediaIds = new HashSet<>();

        for (Rating r : userRatings) {
            knownMediaIds.add(r.getMediaId());

            if (r.getStars() >= 4) {
                Optional<MediaEntry> m = mediaRepo.findById(r.getMediaId());
                if (m.isPresent()) {
                    Collections.addAll(likedGenres, m.get().getGenres());
                }
            }
        }

        if (likedGenres.isEmpty()) {
            return new ArrayList<>();
        }

        List<MediaEntry> candidates = mediaRepo.findByGenres(new ArrayList<>(likedGenres));

        List<MediaEntry> recommendations = new ArrayList<>();
        for (MediaEntry m : candidates) {
            if (!knownMediaIds.contains(m.getId())) {
                recommendations.add(m);
            }
        }

        return recommendations;
    }
}
