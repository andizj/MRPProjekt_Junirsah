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
        r.setVisible(false);
        return repo.save(r);
    }

    public void confirmRating(int ratingId, int userId) {
        Optional<Rating> existing = repo.findById(ratingId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Rating not found");
        }
        Rating r = existing.get();

        if (r.getUserId() != userId) {
            throw new SecurityException("Not your rating");
        }

        r.setVisible(true);
        repo.save(r);
    }

    public Rating updateRating(Rating r, int userId) {
        Optional<Rating> existing = repo.findById(r.getId());

        if (existing.isEmpty()) throw new IllegalArgumentException("Rating not found");
        Rating oldRating = existing.get();

        if (oldRating.getUserId() != userId)
            throw new SecurityException("Not your rating");

        r.setVisible(oldRating.isVisible());

        return repo.save(r);
    }

    public void deleteRating(int id, int userId) {
        Optional<Rating> existing = repo.findById(id);
        if (existing.isEmpty()) throw new IllegalArgumentException("Rating not found");
        if (existing.get().getUserId() != userId)
            throw new SecurityException("Not your rating");

        repo.delete(id);
    }

    public List<Rating> getRatingsForMedia(int mediaId) {
        return repo.findByMediaId(mediaId);
    }

    public List<Rating> getRatingsByUser(int userId) {
        return repo.findByUserId(userId);
    }

    public double getAverageRating(int mediaId) {
        List<Rating> ratings = repo.findByMediaId(mediaId);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Rating r : ratings) {
            sum += r.getStars();
        }
        return sum / ratings.size();
    }
}