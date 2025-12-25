package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.model.UserProfileStats;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class UserProfileHandler implements HttpHandler {

    private final UserService userService;
    private final AuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserProfileHandler(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        setCORSHeaders(ex);
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        String path = ex.getRequestURI().getPath();
        // Erwartet: GET /api/users/{username}/profile
        if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && path.matches("/api/users/[^/]+/profile")) {
            handleGetProfile(ex);
            return;
        }

        send(ex, 404, "{\"error\":\"Not found\"}");
    }

    private void handleGetProfile(HttpExchange ex) throws IOException {
        Optional<User> requester = getUserFromHeader(ex);
        if (requester.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        // Username aus URL extrahieren
        // /api/users/ANDI/profile -> ANDI
        String path = ex.getRequestURI().getPath();
        String username = path.replace("/api/users/", "").replace("/profile", "");

        try {
            UserProfileStats stats = userService.getUserProfile(username);
            send(ex, 200, mapper.writeValueAsString(stats));
        } catch (IllegalArgumentException e) {
            send(ex, 404, "{\"error\":\"User not found\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Internal error\"}");
        }
    }

    private Optional<User> getUserFromHeader(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring("Bearer ".length());
        return authService.validateToken(token);
    }

    private void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}