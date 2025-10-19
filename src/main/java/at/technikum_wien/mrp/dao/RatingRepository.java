package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.Rating;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RatingRepository implements RatingRepositoryIF {

    private final Map<Integer, Rating> ratings = new HashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Override
    public Rating save(Rating rating) {
        if (rating.getId() == 0) {
            rating.setId(idCounter.getAndIncrement());
        }
        ratings.put(rating.getId(), rating);
        return rating;
    }

    @Override
    public List<Rating> findByMediaId(int mediaId) {
        return ratings.values().stream()
                .filter(r -> r.getMediaId() == mediaId)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Rating> findByUserAndMedia(int userId, int mediaId) {
        return ratings.values().stream()
                .filter(r -> r.getUserId() == userId && r.getMediaId() == mediaId)
                .findFirst();
    }
}
