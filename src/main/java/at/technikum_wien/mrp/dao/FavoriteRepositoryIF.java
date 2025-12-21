package at.technikum_wien.mrp.dao;

import java.util.List;

public interface FavoriteRepositoryIF {
    void addFavorite(int userId, int mediaId);
    void removeFavorite(int userId, int mediaId);
    boolean isFavorite(int userId, int mediaId);
    List<Integer> findFavoritesByUserId(int userId);
}