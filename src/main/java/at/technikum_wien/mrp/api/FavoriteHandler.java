package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class FavoriteHandler implements HttpHandler {

    private final MediaService mediaService;
    private final AuthService authService;
    private final ObjectMapper mapper;

    public FavoriteHandler(MediaService mediaService, AuthService authService) {
        this.mediaService = mediaService;
        this.authService = authService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        setCORSHeaders(ex);

        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            // 1. Liste aller Favoriten holen: GET /api/favorites
            if (method.equalsIgnoreCase("GET") && path.equals("/api/favorites")) {
                handleGetFavorites(ex);
                return;
            }

            // 2. Favorit hinzufügen: POST /api/favorites/{id}
            if (method.equalsIgnoreCase("POST") && path.matches("/api/favorites/\\d+")) {
                handleToggleFavorite(ex, true);
                return;
            }

            // 3. Favorit entfernen: DELETE /api/favorites/{id}
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
        Optional<User> user = getUserFromHeader(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        List<MediaEntry> favorites = mediaService.getFavorites(user.get().getId());
        send(ex, 200, mapper.writeValueAsString(favorites));
    }

    private void handleToggleFavorite(HttpExchange ex, boolean add) throws IOException {
        Optional<User> user = getUserFromHeader(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        // ID aus der URL extrahieren (alles nach dem letzten slash)
        String path = ex.getRequestURI().getPath();
        int mediaId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

        try {
            if (add) {
                mediaService.addFavorite(mediaId, user.get().getId());
                send(ex, 201, "{\"message\":\"Added to favorites\"}");
            } else {
                mediaService.removeFavorite(mediaId, user.get().getId());
                send(ex, 204, "");
            }
        } catch (IllegalArgumentException e) {
            send(ex, 404, "{\"error\":\"Media not found\"}");
        }
    }

    private Optional<User> getUserFromHeader(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring("Bearer ".length());
        return authService.validateToken(token);
    }

    private void setCORSHeaders(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}