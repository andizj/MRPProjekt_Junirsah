package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.Rating;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.MediaService;
import at.technikum_wien.mrp.service.RatingService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MediaHandler extends BaseHandler { // 1. Erben

    private final MediaService mediaService;
    private final RatingService ratingService;

    public MediaHandler(AuthService authService, MediaService mediaService, RatingService ratingService) {
        super(authService);
        this.mediaService = mediaService;
        this.ratingService = ratingService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isOptionsRequest(exchange)) return;

        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // GET /api/media
            if (method.equalsIgnoreCase("GET") && (path.equals("/api/media") || path.equals("/api/media/"))) {
                handleGetAllOrFilter(exchange);
                return;
            }
            // GET /api/media/recommendations
            if (method.equalsIgnoreCase("GET") && path.startsWith("/api/media/recommendations")) {
                handleGetRecommendations(exchange);
                return;
            }
            // GET /api/media/{id}
            if (method.equalsIgnoreCase("GET") && path.matches("/api/media/\\d+")) {
                handleGetOne(exchange);
                return;
            }
            // POST /api/media
            if (method.equalsIgnoreCase("POST") && (path.equals("/api/media") || path.equals("/api/media/"))) {
                handleCreate(exchange);
                return;
            }
            // PUT /api/media/{id}
            if (method.equalsIgnoreCase("PUT") && path.matches("/api/media/\\d+")) {
                handleUpdate(exchange);
                return;
            }
            // DELETE /api/media/{id}
            if (method.equalsIgnoreCase("DELETE") && path.matches("/api/media/\\d+")) {
                handleDelete(exchange);
                return;
            }

            if (method.equalsIgnoreCase("POST") && path.matches("/api/media/\\d+/rate")) {
                handleRateMedia(exchange);
                return;
            }

            send(exchange, 404, "{\"error\":\"Endpoint not found\"}");

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void handleGetAllOrFilter(HttpExchange exchange) throws IOException {
        Optional<User> user = getUser(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        Map<String, String> queryParams = getQueryMap(exchange.getRequestURI().getQuery());

        String search = queryParams.get("search");
        String type = queryParams.get("type");
        String genre = queryParams.get("genre");
        String sortBy = queryParams.get("sort");

        Integer year = null;
        if (queryParams.containsKey("year")) {
            try { year = Integer.parseInt(queryParams.get("year")); } catch (NumberFormatException ignored) {}
        }

        Integer minAge = null;
        if (queryParams.containsKey("minAge")) {
            try { minAge = Integer.parseInt(queryParams.get("minAge")); } catch (NumberFormatException ignored) {}
        }

        List<MediaEntry> results = mediaService.getFiltered(search, type, genre, year, minAge, sortBy);
        send(exchange, 200, mapper.writeValueAsString(results));
    }

    private void handleGetRecommendations(HttpExchange exchange) throws IOException {
        Optional<User> user = getUser(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        List<MediaEntry> recs = mediaService.getRecommendations(user.get().getId());
        send(exchange, 200, mapper.writeValueAsString(recs));
    }

    private void handleGetOne(HttpExchange exchange) throws IOException {
        Optional<User> user = getUser(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int id = extractId(exchange.getRequestURI().getPath());
        Optional<MediaEntry> entry = mediaService.getById(id);
        if (entry.isPresent()) {
            send(exchange, 200, mapper.writeValueAsString(entry.get()));
        } else {
            send(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        Optional<User> user = getUser(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        try {
            MediaEntry input = mapper.readValue(exchange.getRequestBody(), MediaEntry.class);
            input.setCreatorId(user.get().getId());
            MediaEntry created = mediaService.create(input);
            send(exchange, 201, mapper.writeValueAsString(created));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleUpdate(HttpExchange exchange) throws IOException {
        Optional<User> user = getUser(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int id = extractId(exchange.getRequestURI().getPath());
        try {
            MediaEntry input = mapper.readValue(exchange.getRequestBody(), MediaEntry.class);
            input.setId(id);
            mediaService.update(input, user.get().getId());
            send(exchange, 200, mapper.writeValueAsString(input));
        } catch (SecurityException e) {
            send(exchange, 403, "{\"error\":\"Forbidden: Not your media\"}");
        } catch (IllegalArgumentException e) {
            send(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        Optional<User> user = getUser(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int id = extractId(exchange.getRequestURI().getPath());
        try {
            mediaService.delete(id, user.get().getId());
            send(exchange, 204);
        } catch (SecurityException e) {
            send(exchange, 403, "{\"error\":\"Forbidden: Not your media\"}");
        } catch (IllegalArgumentException e) {
            send(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleRateMedia(HttpExchange exchange) throws IOException {

        Optional<User> user = getUser(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        int mediaId = extractId(exchange.getRequestURI().getPath(), "/rate");

        try {
            Rating r = mapper.readValue(exchange.getRequestBody(), Rating.class);

            r.setMediaId(mediaId);

            if (r.getStars() < 1 || r.getStars() > 5) {
                send(exchange, 400, "{\"error\":\"Stars must be 1-5\"}");
                return;
            }

            Rating saved = ratingService.addRating(r, user.get().getId());

            send(exchange, 201, mapper.writeValueAsString(saved));

        } catch (IllegalArgumentException e) {
            send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}