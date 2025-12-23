package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.Rating;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.RatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class RatingHandler implements HttpHandler {

    private final RatingService ratingService;
    private final AuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public RatingHandler(RatingService ratingService, AuthService authService) {
        this.ratingService = ratingService;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        enableCORS(ex);

        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            // POST /api/ratings
            if (method.equals("POST") && (path.equals("/api/ratings") || path.equals("/api/ratings/"))) {
                handleCreate(ex);
                return;
            }

            // GET /api/ratings/average/{mediaId}
            if (method.equals("GET") && path.matches("/api/ratings/average/\\d+")) {
                handleGetAverage(ex);
                return;
            }

            // PUT /api/ratings/{id}
            if (method.equals("PUT") && path.matches("/api/ratings/\\d+")) {
                handleUpdate(ex);
                return;
            }

            // DELETE /api/ratings/{id}
            if (method.equals("DELETE") && path.matches("/api/ratings/\\d+")) {
                handleDelete(ex);
                return;
            }

            send(ex, 404, "{\"error\":\"Unknown endpoint\"}");

        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Server error\"}");
        }
    }

    private void handleCreate(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        // Wir lesen die mediaId direkt aus dem JSON Body, nicht aus der URL
        Rating r = mapper.readValue(ex.getRequestBody(), Rating.class);

        // Kleine Validierung
        if (r.getMediaId() <= 0) {
            send(ex, 400, "{\"error\":\"mediaId missing\"}");
            return;
        }
        if (r.getStars() < 1 || r.getStars() > 5) {
            send(ex, 400, "{\"error\":\"stars must be 1-5\"}");
            return;
        }

        Rating saved = ratingService.addRating(r, user.get().getId());
        send(ex, 201, mapper.writeValueAsString(saved));
    }

    private void handleGetAverage(HttpExchange ex) throws IOException {
        // ID aus URL extrahieren: /api/ratings/average/123 -> 123
        String path = ex.getRequestURI().getPath();
        int mediaId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

        double avg = ratingService.getAverageRating(mediaId);

        // Gibt Zahl als String zurück
        send(ex, 200, String.valueOf(avg));
    }

    private void handleUpdate(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        int ratingId = Integer.parseInt(ex.getRequestURI().getPath().replace("/api/ratings/", ""));
        Rating r = mapper.readValue(ex.getRequestBody(), Rating.class);
        r.setId(ratingId);

        try {
            Rating updated = ratingService.updateRating(r, user.get().getId());
            send(ex, 200, mapper.writeValueAsString(updated));
        } catch (SecurityException se) {
            send(ex, 403, "{\"error\":\"Not your rating\"}");
        } catch (IllegalArgumentException iae) {
            send(ex, 404, "{\"error\":\"Rating not found\"}");
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        int ratingId = Integer.parseInt(ex.getRequestURI().getPath().replace("/api/ratings/", ""));

        try {
            ratingService.deleteRating(ratingId, user.get().getId());
            send(ex, 204, "");
        } catch (SecurityException se) {
            send(ex, 403, "{\"error\":\"Not your rating\"}");
        } catch (IllegalArgumentException iae) {
            send(ex, 404, "{\"error\":\"Rating not found\"}");
        }
    }

    private Optional<User> getUser(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring("Bearer ".length());
        return authService.validateToken(token);
    }

    private void enableCORS(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
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