package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.LeaderboardEntry;
import at.technikum_wien.mrp.service.AuthService; // Wichtig: Importieren!
import at.technikum_wien.mrp.service.RatingService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

public class LeaderboardHandler extends BaseHandler {

    private final RatingService ratingService;

    public LeaderboardHandler(RatingService ratingService, AuthService authService) {
        super(authService);
        this.ratingService = ratingService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (isOptionsRequest(ex)) return;

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
}