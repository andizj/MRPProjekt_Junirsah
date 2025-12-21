package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.RatingRepositoryIF;
import at.technikum_wien.mrp.model.Rating;

import java.util.List;
import java.util.Optional;

public class RatingService {

    private final RatingRepositoryIF repo;

    public RatingService(RatingRepositoryIF repo) {
        this.repo = repo;
    }

    public Rating addRating(Rating r, int userId) {
        r.setUserId(userId);
        return repo.save(r);
    }

    public Rating updateRating(Rating r, int userId) {
        Optional<Rating> existing = repo.findById(r.getId());

        if (existing.isEmpty()) throw new IllegalArgumentException("Rating existiert nicht");
        if (existing.get().getUserId() != userId)
            throw new SecurityException("Nicht dein Rating");

        return repo.save(r);
    }

    public void deleteRating(int id, int userId) {
        Optional<Rating> existing = repo.findById(id);
        if (existing.isEmpty()) throw new IllegalArgumentException("Rating existiert nicht");
        if (existing.get().getUserId() != userId)
            throw new SecurityException("Nicht dein Rating");

        repo.delete(id);
    }

    public List<Rating> getRatingsForMedia(int mediaId) {
        return repo.findByMediaId(mediaId);
    }

    public List<Rating> getRatingsByUser(int userId) {
        return repo.findByUserId(userId);
    }
}
