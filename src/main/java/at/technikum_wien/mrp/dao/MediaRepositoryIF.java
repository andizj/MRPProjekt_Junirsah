package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.MediaEntry;
import java.util.*;

public interface MediaRepositoryIF {
    MediaEntry save(MediaEntry entry);
    Optional<MediaEntry> findById(int id);
    List<MediaEntry> findAll();
    void delete(int id);
}
