package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.MediaService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FavoriteHandler extends BaseHandler { // 1. Erben

    private final MediaService mediaService;
    // authService und mapper kommen aus BaseHandler

    public FavoriteHandler(MediaService mediaService, AuthService authService) {
        super(authService); // 2. Super-Konstruktor
        this.mediaService = mediaService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // 3. Preflight Check
        if (isOptionsRequest(ex)) return;

        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            // GET /api/favorites
            if (method.equalsIgnoreCase("GET") && path.equals("/api/favorites")) {
                handleGetFavorites(ex);
                return;
            }
            // POST /api/favorites/{id}
            if (method.equalsIgnoreCase("POST") && path.matches("/api/favorites/\\d+")) {
                handleToggleFavorite(ex, true);
                return;
            }
            // DELETE /api/favorites/{id}
            if (method.equalsIgnoreCase("DELETE") && path.matches("/api/favorites/\\d+")) {
                handleToggleFavorite(ex, false);
                return;
            }

            send(ex, 404, "{\"error\":\"Endpoint not found\"}");

        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void handleGetFavorites(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex); // Aus BaseHandler
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        List<MediaEntry> favorites = mediaService.getFavorites(user.get().getId());
        send(ex, 200, mapper.writeValueAsString(favorites));
    }

    private void handleToggleFavorite(HttpExchange ex, boolean add) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        int mediaId = extractId(ex.getRequestURI().getPath()); // Aus BaseHandler

        try {
            if (add) {
                mediaService.addFavorite(mediaId, user.get().getId());
                send(ex, 201, "{\"message\":\"Added to favorites\"}");
            } else {
                mediaService.removeFavorite(mediaId, user.get().getId());
                send(ex, 204);
            }
        } catch (IllegalArgumentException e) {
            send(ex, 404, "{\"error\":\"Media not found\"}");
        }
    }
}