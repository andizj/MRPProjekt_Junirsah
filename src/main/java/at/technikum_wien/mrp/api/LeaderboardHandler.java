package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.LeaderboardEntry;
import at.technikum_wien.mrp.service.RatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LeaderboardHandler implements HttpHandler {

    private final RatingService ratingService;
    private final ObjectMapper mapper = new ObjectMapper();

    public LeaderboardHandler(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // CORS Header
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 404, "{\"error\":\"Only GET supported\"}");
            return;
        }

        try {
            List<LeaderboardEntry> list = ratingService.getLeaderboard();
            send(ex, 200, mapper.writeValueAsString(list));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Internal error\"}");
        }
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}