package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.Rating;
import java.util.*;

public interface RatingRepositoryIF {
    Rating save(Rating rating);
    Optional<Rating> findById(int id);
    List<Rating> findByMediaId(int mediaId);
    List<Rating> findByUserId(int userId);
    void delete(int id);

    void addLike(int userId, int ratingId);
    void removeLike(int userId, int ratingId);
    int countLikes(int ratingId);

    int countRatingsByUserId(int userId);
    double getAverageRatingByUserId(int userId);

    List<at.technikum_wien.mrp.model.LeaderboardEntry> getLeaderboard();
}
