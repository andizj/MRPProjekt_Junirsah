package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UserLoginHandler implements HttpHandler {

    private final AuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserLoginHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        try {
            LoginRequest req = mapper.readValue(exchange.getRequestBody(), LoginRequest.class);
            String token = authService.login(req.username, req.password);
            String json = mapper.writeValueAsString(new LoginResponse(token));
            sendJson(exchange, 200, json);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 401, "{\"error\":\"invalid credentials\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"internal server error\"}");
        }
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static class LoginRequest {
        public String username;
        public String password;
    }

    private static class LoginResponse {
        public String token;
        public LoginResponse(String token) { this.token = token; }
    }
}

