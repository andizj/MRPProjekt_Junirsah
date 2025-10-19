package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UserRegisterHandler implements HttpHandler {

    private final UserService userService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserRegisterHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        try {
            RegistrationRequest req = mapper.readValue(exchange.getRequestBody(), RegistrationRequest.class);
            User user = userService.register(req.username, req.password);
            String json = mapper.writeValueAsString(new RegistrationResponse("user created", user.getUsername()));
            sendResponse(exchange, 201, json);

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"internal Server Error\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
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
