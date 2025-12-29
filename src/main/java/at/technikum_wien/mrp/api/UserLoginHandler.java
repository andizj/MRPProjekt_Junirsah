package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.service.AuthService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class UserLoginHandler extends BaseHandler { // 1. Erben

    // mapper und authService kommen aus BaseHandler

    public UserLoginHandler(AuthService authService) {
        super(authService); // 2. Super-Konstruktor
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 3. Preflight Check One-Liner
        if (isOptionsRequest(exchange)) return;

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        try {
            LoginRequest req = mapper.readValue(exchange.getRequestBody(), LoginRequest.class);
            String token = authService.login(req.username, req.password);
            String json = mapper.writeValueAsString(new LoginResponse(token));
            send(exchange, 200, json);
        } catch (IllegalArgumentException e) {
            send(exchange, 401, "{\"error\":\"invalid credentials\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"internal server error\"}");
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