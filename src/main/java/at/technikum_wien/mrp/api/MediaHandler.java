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
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        // /api/media or /api/media/{id}
        try {
            if (method.equalsIgnoreCase("POST") && path.equals("/api/media")) {
                handleCreate(ex);
            } else if (method.equalsIgnoreCase("GET") && path.equals("/api/media")) {
                handleGetAll(ex);
            } else if (method.equalsIgnoreCase("GET") && path.matches("/api/media/\\d+")) {
                handleGet(ex);
            } else if (method.equalsIgnoreCase("PUT") && path.matches("/api/media/\\d+")) {
                handleUpdate(ex);
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/api/media/\\d+")) {
                handleDelete(ex);
            } else {
                send(ex, 404, "{\"error\":\"unknown endpoint\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"server error\"}");
        }
    }

    private void handleCreate(HttpExchange ex) throws IOException {
        Optional<User> user = getUserFromHeader(ex);
        if (user.isEmpty()) { send(ex, 401, "{\"error\":\"unauthorized\"}"); return; }

        MediaEntry entry = mapper.readValue(ex.getRequestBody(), MediaEntry.class);
        entry.setCreatorId(user.get().getId());
        MediaEntry saved = mediaService.create(entry);
        send(ex, 201, mapper.writeValueAsString(saved));
    }

    private void handleGet(HttpExchange ex) throws IOException {
        int id = Integer.parseInt(ex.getRequestURI().getPath().replace("/api/media/", ""));
        Optional<MediaEntry> m = mediaService.getById(id);
        if (m.isEmpty()) { send(ex, 404, "{\"error\":\"not found\"}"); return; }
        send(ex, 200, mapper.writeValueAsString(m.get()));
    }

    private void handleGetAll(HttpExchange ex) throws IOException {
        send(ex, 200, mapper.writeValueAsString(mediaService.getAll()));
    }

    private void handleUpdate(HttpExchange ex) throws IOException {
        Optional<User> user = getUserFromHeader(ex);
        if (user.isEmpty()) { send(ex, 401, "{\"error\":\"unauthorized\"}"); return; }

        int id = Integer.parseInt(ex.getRequestURI().getPath().replace("/api/media/", ""));
        MediaEntry entry = mapper.readValue(ex.getRequestBody(), MediaEntry.class);
        entry.setId(id);
        try {
            mediaService.update(entry, user.get().getId());
            send(ex, 200, mapper.writeValueAsString(entry));
        } catch (SecurityException se) {
            send(ex, 403, "{\"error\":\"not your media\"}");
        } catch (IllegalArgumentException iae) {
            send(ex, 404, "{\"error\":\"not found\"}");
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        Optional<User> user = getUserFromHeader(ex);
        if (user.isEmpty()) { send(ex, 401, "{\"error\":\"unauthorized\"}"); return; }

        int id = Integer.parseInt(ex.getRequestURI().getPath().replace("/api/media/", ""));
        try {
            mediaService.delete(id, user.get().getId());
            send(ex, 204, "");
        } catch (SecurityException se) {
            send(ex, 403, "{\"error\":\"not your media\"}");
        } catch (IllegalArgumentException iae) {
            send(ex, 404, "{\"error\":\"not found\"}");
        }
    }

    private Optional<User> getUserFromHeader(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring("Bearer ".length());
        return authService.validateToken(token);
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}

