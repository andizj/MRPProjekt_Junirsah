package at.technikum_wien.mrp.dao.impl;

import at.technikum_wien.mrp.dao.interfaces.UserRepositoryIF;
import at.technikum_wien.mrp.database.DatabaseConnectionIF;
import at.technikum_wien.mrp.model.User;
import java.sql.*;
import java.util.Optional;

public class UserRepository implements UserRepositoryIF {

    private final DatabaseConnectionIF dbProvider;

    public UserRepository(DatabaseConnectionIF dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    public User save(User user) {
        if (user.getId() > 0) {
            return update(user);
        } else {
            return insert(user);
        }
    }

    private User insert(User user) {
        String sql = "INSERT INTO users (username, password_hash, email, favorite_genre) VALUES (?, ?, ?, ?) RETURNING id, created_at";

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFavoriteGenre());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getInt("id"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        user.setCreatedAt(ts.toLocalDateTime());
                    }
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving new user", e);
        }
    }

    private User update(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, favorite_genre = ? WHERE id = ?";

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getFavoriteGenre());

            ps.setInt(4, user.getId());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Update failed: User with ID " + user.getId() + " not found.");
            }

            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking user existence", e);
        }
        return false;
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setFavoriteGenre(rs.getString("favorite_genre"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }

        return user;
    }
}