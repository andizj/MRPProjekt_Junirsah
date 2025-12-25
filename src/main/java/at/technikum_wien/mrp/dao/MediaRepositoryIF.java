package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.MediaEntry;
import java.util.*;

public interface MediaRepositoryIF {
    MediaEntry save(MediaEntry entry);
    Optional<MediaEntry> findById(int id);
    List<MediaEntry> findAll();
    List<MediaEntry> findAll(String search, String type, String genre, Integer year, Integer minAge, String sortBy);
    List<MediaEntry> findByGenres(List<String> genres);
    void delete(int id);
}
