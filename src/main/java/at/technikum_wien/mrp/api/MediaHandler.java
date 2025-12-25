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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MediaHandler implements HttpHandler {

    private final AuthService authService;
    private final MediaService mediaService;
    private final ObjectMapper mapper;

    public MediaHandler(AuthService authService, MediaService mediaService) {
        this.authService = authService;
        this.mediaService = mediaService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        setCORSHeaders(exchange);
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // GET /api/media (Alle oder Filter)
            if (method.equalsIgnoreCase("GET") && (path.equals("/api/media") || path.equals("/api/media/"))) {
                handleGetAllOrFilter(exchange);
                return;
            }
            // GET /api/media/recommendations
            if (method.equalsIgnoreCase("GET") && (path.equals("/api/media/recommendations") || path.equals("/api/media/recommendations/"))) {
                handleGetRecommendations(exchange);
                return;
            }
            // GET /api/media/{id}
            if (method.equalsIgnoreCase("GET") && path.matches("/api/media/\\d+")) {
                handleGetOne(exchange);
                return;
            }
            // POST /api/media (Erstellen)
            if (method.equalsIgnoreCase("POST") && (path.equals("/api/media") || path.equals("/api/media/"))) {
                handleCreate(exchange);
                return;
            }
            // PUT /api/media/{id} (Update)
            if (method.equalsIgnoreCase("PUT") && path.matches("/api/media/\\d+")) {
                handleUpdate(exchange);
                return;
            }
            // DELETE /api/media/{id} (Löschen)
            if (method.equalsIgnoreCase("DELETE") && path.matches("/api/media/\\d+")) {
                handleDelete(exchange);
                return;
            }

            send(exchange, 404, "{\"error\":\"Endpoint not found\"}");

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    private void handleGetAllOrFilter(HttpExchange exchange) throws IOException {
        Optional<User> user = getUserFromHeader(exchange);
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

        String json = mapper.writeValueAsString(results);
        send(exchange, 200, json);
    }

    private void handleGetRecommendations(HttpExchange exchange) throws IOException {
        Optional<User> user = getUserFromHeader(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        List<MediaEntry> recs = mediaService.getRecommendations(user.get().getId());
        send(exchange, 200, mapper.writeValueAsString(recs));
    }

    private Map<String, String> getQueryMap(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length > 1) {
                map.put(keyValue[0], keyValue[1]);
            } else if (keyValue.length == 1) {
                map.put(keyValue[0], "");
            }
        }
        return map;
    }

    private void handleGetOne(HttpExchange exchange) throws IOException {
        Optional<User> user = getUserFromHeader(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int id = getIdFromPath(exchange.getRequestURI().getPath());
        Optional<MediaEntry> entry = mediaService.getById(id);
        if (entry.isPresent()) {
            send(exchange, 200, mapper.writeValueAsString(entry.get()));
        } else {
            send(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        Optional<User> user = getUserFromHeader(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        try {
            MediaEntry input = mapper.readValue(exchange.getRequestBody(), MediaEntry.class);
            // Creator ID setzen (vom eingeloggten User)
            input.setCreatorId(user.get().getId());

            MediaEntry created = mediaService.create(input);
            send(exchange, 201, mapper.writeValueAsString(created));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleUpdate(HttpExchange exchange) throws IOException {
        Optional<User> user = getUserFromHeader(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int id = getIdFromPath(exchange.getRequestURI().getPath());
        try {
            MediaEntry input = mapper.readValue(exchange.getRequestBody(), MediaEntry.class);
            input.setId(id); // ID aus URL nehmen
            mediaService.update(input, user.get().getId());
            send(exchange, 200, mapper.writeValueAsString(input));
        } catch (SecurityException e) {
            send(exchange, 403, "{\"error\":\"Forbidden: Not your media\"}");
        } catch (IllegalArgumentException e) {
            send(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        Optional<User> user = getUserFromHeader(exchange);
        if (user.isEmpty()) {
            send(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int id = getIdFromPath(exchange.getRequestURI().getPath());
        try {
            mediaService.delete(id, user.get().getId());
            send(exchange, 204, "");
        } catch (SecurityException e) {
            send(exchange, 403, "{\"error\":\"Forbidden: Not your media\"}");
        } catch (IllegalArgumentException e) {
            send(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private int getIdFromPath(String path) {
        return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    }

    private Optional<User> getUserFromHeader(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring("Bearer ".length());
        return authService.validateToken(token);
    }

    private void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}