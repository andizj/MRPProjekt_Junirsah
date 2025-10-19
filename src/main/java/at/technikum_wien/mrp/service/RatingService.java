package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.RatingRepository;
import at.technikum_wien.mrp.dao.RatingRepositoryIF;
import at.technikum_wien.mrp.model.Rating;
import java.util.List;

public class RatingService {

    private final RatingRepositoryIF ratingRepo;

    public RatingService(RatingRepositoryIF ratingRepo) {
        this.ratingRepo = ratingRepo;
    }

    public Rating addRating(int mediaId, int userId, int score, String comment) {
        Rating rating = new Rating(0, mediaId, userId, score, comment);
        return ratingRepo.save(rating);
    }

    public List<Rating> getRatingsForMedia(int mediaId) {
        return ratingRepo.findByMediaId(mediaId);
    }
}
