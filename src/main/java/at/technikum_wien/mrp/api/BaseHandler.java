package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public abstract class BaseHandler implements HttpHandler {

    protected final AuthService authService;
    protected final ObjectMapper mapper;

    public BaseHandler(AuthService authService) {
        this.authService = authService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    protected void send(HttpExchange ex, int code, String body) throws IOException {
        setCORSHeaders(ex);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void send(HttpExchange ex, int code) throws IOException {
        setCORSHeaders(ex);
        ex.sendResponseHeaders(code, -1);
    }

    protected void setCORSHeaders(HttpExchange ex) {
        if (!ex.getResponseHeaders().containsKey("Access-Control-Allow-Origin")) {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        }
    }

    protected Optional<User> getUser(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring("Bearer ".length());
        return authService.validateToken(token);
    }

    protected int extractId(String path) {
        try {
            return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    protected int extractId(String path, String suffix) {
        return extractId(path.replace(suffix, ""));
    }

    protected boolean isOptionsRequest(HttpExchange ex) throws IOException {
        setCORSHeaders(ex);
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}