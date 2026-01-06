package at.technikum_wien.mrp.dao.impl;

import at.technikum_wien.mrp.dao.interfaces.MediaRepositoryIF;
import at.technikum_wien.mrp.database.DatabaseConnectionIF;
import at.technikum_wien.mrp.model.MediaEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MediaRepository implements MediaRepositoryIF {

    private final DatabaseConnectionIF dbProvider;

    public MediaRepository(DatabaseConnectionIF dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    public MediaEntry save(MediaEntry entry) {
        if (entry.getId() > 0) {
            return update(entry);
        } else {
            return insert(entry);
        }
    }

    private MediaEntry insert(MediaEntry entry) {
        String sql = """
        INSERT INTO media (title, description, media_type, release_year, genres, age_restriction, creator_id, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
    """;
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entry.getTitle());
            ps.setString(2, entry.getDescription());
            ps.setString(3, entry.getMediaType());
            ps.setInt(4, entry.getReleaseYear());
            ps.setArray(5, conn.createArrayOf("text", entry.getGenres()));
            ps.setInt(6, entry.getAgeRestriction());
            ps.setInt(7, entry.getCreatorId());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    entry.setId(rs.getInt("id"));
                }
            }
            return entry;
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern des Media-Eintrags", e);
        }
    }

    private MediaEntry update(MediaEntry entry) {
        String sql = """
        UPDATE media 
        SET title = ?, description = ?, media_type = ?, release_year = ?, 
            genres = ?, age_restriction = ?, creator_id = ? 
        WHERE id = ?
    """;
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entry.getTitle());
            ps.setString(2, entry.getDescription());
            ps.setString(3, entry.getMediaType());
            ps.setInt(4, entry.getReleaseYear());
            ps.setArray(5, conn.createArrayOf("text", entry.getGenres()));
            ps.setInt(6, entry.getAgeRestriction());
            ps.setInt(7, entry.getCreatorId());

            ps.setInt(8, entry.getId());

            ps.executeUpdate();
            return entry;

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Aktualisieren des Media-Eintrags", e);
        }
    }

    @Override
    public Optional<MediaEntry> findById(int id) {
        String sql = "SELECT * FROM media WHERE id = ?";
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
            throw new RuntimeException("Fehler beim Abrufen des Media-Eintrags", e);
        }
    }

    @Override
    public List<MediaEntry> findAll() {
        String sql = "SELECT * FROM media ORDER BY id";
        List<MediaEntry> result = new ArrayList<>();

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden aller Media-Einträge", e);
        }
        return result;
    }
    @Override
    public List<MediaEntry> findAll(String search, String type, String genre, Integer year, Integer minAge, String sortBy) {
        StringBuilder sql = new StringBuilder("SELECT * FROM media WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND title ILIKE ?");
            params.add("%" + search + "%");
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND media_type = ?");
            params.add(type);
        }
        if (year != null) {
            sql.append(" AND release_year = ?");
            params.add(year);
        }
        if (minAge != null) {
            sql.append(" AND age_restriction >= ?");
            params.add(minAge);
        }
        if (genre != null && !genre.isBlank()) {
            sql.append(" AND ? = ANY(genres)");
            params.add(genre);
        }

        if ("year".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY release_year DESC");
        } else if ("title".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY title ASC");
        } else {
            sql.append(" ORDER BY id ASC");
        }

        List<MediaEntry> result = new ArrayList<>();
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) {
                    ps.setInt(i + 1, (Integer) p);
                } else {
                    ps.setString(i + 1, (String) p);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Filtern der Media-Einträge", e);
        }
        return result;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM media WHERE id = ?";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Löschen des Media-Eintrags", e);
        }
    }

    private MediaEntry map(ResultSet rs) throws SQLException {
        Array genreArray = rs.getArray("genres");
        String[] genres = genreArray != null ? (String[]) genreArray.getArray() : new String[0];

        Timestamp ts = rs.getTimestamp("created_at");

        return new MediaEntry(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("media_type"),
                rs.getInt("release_year"),
                genres,
                rs.getInt("age_restriction"),
                rs.getInt("creator_id"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
    @Override
    public List<MediaEntry> findByGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM media WHERE genres && ?";
        List<MediaEntry> result = new ArrayList<>();

        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Array sqlArray = conn.createArrayOf("text", genres.toArray());
            ps.setArray(1, sqlArray);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding media by genres", e);
        }
        return result;
    }
}
