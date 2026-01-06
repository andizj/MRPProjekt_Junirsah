package at.technikum_wien.mrp.dao.impl;

import at.technikum_wien.mrp.dao.interfaces.RatingRepositoryIF;
import at.technikum_wien.mrp.database.DatabaseConnectionIF;
import at.technikum_wien.mrp.model.Rating;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RatingRepository implements RatingRepositoryIF {

    private final DatabaseConnectionIF dbProvider;

    public RatingRepository(DatabaseConnectionIF dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    public Rating save(Rating rating) {
        if (rating.getId() > 0) {
            return update(rating);
        } else {
            return insert(rating);
        }
    }

    private Rating insert(Rating rating) {
        String sql = "INSERT INTO ratings (media_id, user_id, stars, comment, visible, created_at) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getMediaId());
            ps.setInt(2, rating.getUserId());
            ps.setInt(3, rating.getStars());
            ps.setString(4, rating.getComment());
            ps.setBoolean(5, rating.isVisible());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rating.setId(rs.getInt("id"));
                }
            }
            return rating;
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting rating", e);
        }
    }

    private Rating update(Rating rating) {
        String sql = "UPDATE ratings SET stars = ?, comment = ?, visible = ? WHERE id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getStars());
            ps.setString(2, rating.getComment());
            ps.setBoolean(3, rating.isVisible());
            ps.setInt(4, rating.getId());
            ps.executeUpdate();
            return rating;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating rating", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting rating", e);
        }
    }

    @Override
    public Optional<Rating> findById(int id) {
        String sql = "SELECT * FROM ratings WHERE id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                    }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public List<Rating> findByMediaId(int mediaId) {
        String sql = "SELECT * FROM ratings WHERE media_id = ? AND visible = true";
        List<Rating> list = new ArrayList<>();
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mediaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<Rating> findByUserId(int userId) {
        String sql = "SELECT * FROM ratings WHERE user_id = ?";
        List<Rating> list = new ArrayList<>();
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void addLike(int userId, int ratingId) {
        String sql = "INSERT INTO rating_likes (user_id, rating_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, ratingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error adding like", e);
        }
    }

    @Override
    public void removeLike(int userId, int ratingId) {
        String sql = "DELETE FROM rating_likes WHERE user_id = ? AND rating_id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, ratingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error removing like", e);
        }
    }

    @Override
    public int countLikes(int ratingId) {
        String sql = "SELECT COUNT(*) FROM rating_likes WHERE rating_id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ratingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting likes", e);
        }
        return 0;
    }

    private Rating map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Rating(
                rs.getInt("id"),
                rs.getInt("media_id"),
                rs.getInt("user_id"),
                rs.getInt("stars"),
                rs.getString("comment"),
                rs.getBoolean("visible"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
    @Override
    public int countRatingsByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM ratings WHERE user_id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    @Override
    public double getAverageRatingByUserId(int userId) {
        String sql = "SELECT AVG(stars) FROM ratings WHERE user_id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0.0;
    }
    @Override
    public List<at.technikum_wien.mrp.model.LeaderboardEntry> getLeaderboard() {
        String sql = """
            SELECT u.username, COUNT(r.id) as cnt
            FROM ratings r
            JOIN users u ON r.user_id = u.id
            WHERE r.visible = true
            GROUP BY u.username
            ORDER BY cnt DESC
            LIMIT 10
        """;

        List<at.technikum_wien.mrp.model.LeaderboardEntry> list = new ArrayList<>();

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new at.technikum_wien.mrp.model.LeaderboardEntry(
                        rs.getString("username"),
                        rs.getLong("cnt")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching leaderboard", e);
        }
        return list;
    }
}