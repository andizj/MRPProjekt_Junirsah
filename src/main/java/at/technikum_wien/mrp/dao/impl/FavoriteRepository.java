package at.technikum_wien.mrp.dao.impl;

import at.technikum_wien.mrp.dao.interfaces.FavoriteRepositoryIF;
import at.technikum_wien.mrp.database.DatabaseConnectionIF;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FavoriteRepository implements FavoriteRepositoryIF {

    private final DatabaseConnectionIF dbProvider;

    public FavoriteRepository(DatabaseConnectionIF dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    public void addFavorite(int userId, int mediaId) {
        String sql = "INSERT INTO favorites (user_id, media_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, mediaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Hinzufügen des Favoriten", e);
        }
    }

    @Override
    public void removeFavorite(int userId, int mediaId) {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND media_id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, mediaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Entfernen des Favoriten", e);
        }
    }

    @Override
    public boolean isFavorite(int userId, int mediaId) {
        String sql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND media_id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, mediaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Prüfen des Favoriten", e);
        }
        return false;
    }

    @Override
    public List<Integer> findFavoritesByUserId(int userId) {
        String sql = "SELECT media_id FROM favorites WHERE user_id = ?";
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("media_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Favoriten", e);
        }
        return ids;
    }
}