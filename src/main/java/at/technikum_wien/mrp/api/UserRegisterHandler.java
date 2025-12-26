package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UserRegisterHandler implements HttpHandler {

    private final AuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserRegisterHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // --- 1. CORS Preflight Handling (NEU) ---
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            setCORSHeaders(exchange);
            exchange.sendResponseHeaders(204, -1); // 204 No Content
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        try {
            RegistrationRequest req = mapper.readValue(exchange.getRequestBody(), RegistrationRequest.class);

            // Prüft auf leere Felder vor dem Service-Aufruf
            if (req.username == null || req.username.isBlank() || req.password == null || req.password.isBlank()) {
                sendResponse(exchange, 400, "{\"error\":\"Username and password must not be empty\"}");
                return;
            }

            User user = authService.register(req.username, req.password);

            String json = mapper.writeValueAsString(new RegistrationResponse("User registered", user.getUsername()));
            sendResponse(exchange, 201, json);

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"internal Server Error\"}");
        }
    }

    private void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        // Hinzufügen der CORS-Header zu jeder Antwort
        setCORSHeaders(exchange);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Inner DTOs
    private static class RegistrationRequest {
        public String username;
        public String password;
    }

    private static class RegistrationResponse {
        public String message;
        public String username;
        public RegistrationResponse(String message, String username) {
            this.message = message;
            this.username = username;
        }
    }
}