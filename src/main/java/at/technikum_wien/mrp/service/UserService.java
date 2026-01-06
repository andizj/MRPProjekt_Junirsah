package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.interfaces.MediaRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.RatingRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.UserRepositoryIF;
import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.Rating;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.model.UserProfileStats;

import java.util.*;
import java.util.regex.Pattern;

public class UserService {

    private final UserRepositoryIF userRepo;
    private final RatingRepositoryIF ratingRepo;
    private final MediaRepositoryIF mediaRepo;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public UserService(UserRepositoryIF userRepo, RatingRepositoryIF ratingRepo, MediaRepositoryIF mediaRepo) {
        this.userRepo = userRepo;
        this.ratingRepo = ratingRepo;
        this.mediaRepo = mediaRepo;
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public UserProfileStats getUserProfile(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int count = ratingRepo.countRatingsByUserId(user.getId());
        double avg = ratingRepo.getAverageRatingByUserId(user.getId());
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
                if (media.get().getGenres() != null) {
                    for (String g : media.get().getGenres()) {
                        genreCounts.put(g, genreCounts.getOrDefault(g, 0) + 1);
                    }
                }
            }
        }

        return genreCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
    }

    public void updateUser(String currentUsername, String newUsername, String newEmail, String newFavoriteGenre) {

        User existingUser = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

        if (newUsername != null && !newUsername.isBlank()) {
            if (!newUsername.equals(existingUser.getUsername())) {
                if (userRepo.existsByUsername(newUsername)) {
                    throw new IllegalArgumentException("Username '" + newUsername + "' is already taken.");
                }
                existingUser.setUsername(newUsername);
            }
        }
        if (newEmail != null && !newEmail.isBlank()) {
            if (!EMAIL_PATTERN.matcher(newEmail).matches()) {
                throw new IllegalArgumentException("Invalid email format (e.g. user@example.com)");
            }
            existingUser.setEmail(newEmail);
        }

        if (newFavoriteGenre != null && !newFavoriteGenre.isBlank()) {
            existingUser.setFavoriteGenre(newFavoriteGenre);
        }

        userRepo.save(existingUser);
    }

    public UserProfileStats getUserProfile(int userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int count = ratingRepo.countRatingsByUserId(user.getId());
        double avg = ratingRepo.getAverageRatingByUserId(user.getId());
        String favGenre = calculateFavoriteGenre(user.getId());

        return new UserProfileStats(user.getUsername(), count, avg, favGenre);
    }
    public Optional<User> findById(int id) {
        return userRepo.findById(id);
    }
}