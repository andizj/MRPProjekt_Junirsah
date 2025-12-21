package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.Rating;
import java.util.*;

public interface RatingRepositoryIF {
    Rating save(Rating rating);
    Optional<Rating> findById(int id);
    List<Rating> findByMediaId(int mediaId);
    List<Rating> findByUserId(int userId);
    void delete(int id);
}
