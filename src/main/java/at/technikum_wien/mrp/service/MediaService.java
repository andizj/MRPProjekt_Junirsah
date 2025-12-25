package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.FavoriteRepositoryIF;
import at.technikum_wien.mrp.dao.MediaRepository;
import at.technikum_wien.mrp.dao.MediaRepositoryIF;
import at.technikum_wien.mrp.dao.RatingRepositoryIF;
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
    //Neu
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
