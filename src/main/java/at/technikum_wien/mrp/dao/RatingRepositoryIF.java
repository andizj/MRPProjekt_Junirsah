package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.Rating;
import java.util.*;

public interface RatingRepositoryIF {
    Rating save(Rating rating);
    List<Rating> findByMediaId(int mediaId);
    Optional<Rating> findByUserAndMedia(int userId, int mediaId);
}
