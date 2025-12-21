package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.Rating;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class RatingRepository implements RatingRepositoryIF {

    private final DatabaseConnection dbProvider;

    public RatingRepository(DatabaseConnection dbProvider) {
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
        String sql = """
            INSERT INTO ratings (media_id, user_id, stars, comment, created_at)
            VALUES (?, ?, ?, ?, ?) RETURNING id
        """;

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, rating.getMediaId());
            ps.setInt(2, rating.getUserId());
            ps.setInt(3, rating.getStars());
            ps.setString(4, rating.getComment());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rating.setId(rs.getInt("id"));
                }
            }

            return rating;

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Erstellen eines Ratings", e);
        }
    }

    private Rating update(Rating rating) {
        String sql = """
            UPDATE ratings
            SET stars = ?, comment = ?
            WHERE id = ?
        """;

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, rating.getStars());
            ps.setString(2, rating.getComment());
            ps.setInt(3, rating.getId());

            ps.executeUpdate();
            return rating;

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Aktualisieren eines Ratings", e);
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
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Abrufen eines Ratings", e);
        }
    }

    @Override
    public List<Rating> findByMediaId(int mediaId) {
        String sql = "SELECT * FROM ratings WHERE media_id = ? ORDER BY created_at DESC";

        List<Rating> list = new ArrayList<>();

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, mediaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Ratings zu media_id=" + mediaId, e);
        }

        return list;
    }

    @Override
    public List<Rating> findByUserId(int userId) {
        String sql = "SELECT * FROM ratings WHERE user_id = ? ORDER BY created_at DESC";

        List<Rating> list = new ArrayList<>();

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Ratings eines Users", e);
        }

        return list;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM ratings WHERE id = ?";

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Löschen eines Ratings", e);
        }
    }

    // Mapping-Methode
    private Rating map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");

        return new Rating(
                rs.getInt("id"),
                rs.getInt("media_id"),
                rs.getInt("user_id"),
                rs.getInt("stars"),
                rs.getString("comment"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
