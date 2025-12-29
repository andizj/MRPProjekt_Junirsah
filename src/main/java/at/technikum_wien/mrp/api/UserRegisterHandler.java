package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class UserRegisterHandler extends BaseHandler {

    public UserRegisterHandler(AuthService authService) {
        super(authService);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isOptionsRequest(exchange)) return;

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        try {
            RegistrationRequest req = mapper.readValue(exchange.getRequestBody(), RegistrationRequest.class);

            if (req.username == null || req.username.isBlank() || req.password == null || req.password.isBlank()) {
                send(exchange, 400, "{\"error\":\"Username and password must not be empty\"}");
                return;
            }

            User user = authService.register(req.username, req.password);
            String json = mapper.writeValueAsString(new RegistrationResponse("User registered", user.getUsername()));
            send(exchange, 201, json);

        } catch (IllegalArgumentException e) {
            send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"internal Server Error\"}");
        }
    }

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