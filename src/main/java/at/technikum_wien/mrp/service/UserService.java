package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.MediaRepositoryIF;
import at.technikum_wien.mrp.dao.RatingRepositoryIF;
import at.technikum_wien.mrp.dao.UserRepositoryIF;
import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.Rating;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.model.UserProfileStats;

import java.util.*;

public class UserService {

    private final UserRepositoryIF userRepo;
    private final RatingRepositoryIF ratingRepo;
    private final MediaRepositoryIF mediaRepo;

    public UserService(UserRepositoryIF userRepo, RatingRepositoryIF ratingRepo, MediaRepositoryIF mediaRepo) {
        this.userRepo = userRepo;
        this.ratingRepo = ratingRepo;
        this.mediaRepo = mediaRepo;
    }

    public User register(User user) {
        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        return userRepo.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public UserProfileStats getUserProfile(String username) {
        // 1. User finden
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Einfache Stats aus der DB
        int count = ratingRepo.countRatingsByUserId(user.getId());
        double avg = ratingRepo.getAverageRatingByUserId(user.getId());

        // 3. Lieblingsgenre berechnen
        String favGenre = calculateFavoriteGenre(user.getId());

        return new UserProfileStats(user.getUsername(), count, avg, favGenre);
    }

    private String calculateFavoriteGenre(int userId) {
        List<Rating> ratings = ratingRepo.findByUserId(userId);
        if (ratings.isEmpty()) return "None";

        Map<String, Integer> genreCounts = new HashMap<>();

        for (Rating r : ratings) {
            Optional<MediaEntry> media = mediaRepo.findById(r.getMediaId());
            if (media.isPresent()) {
                for (String g : media.get().getGenres()) {
                    genreCounts.put(g, genreCounts.getOrDefault(g, 0) + 1);
                }
            }
        }

        return genreCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
    }
}