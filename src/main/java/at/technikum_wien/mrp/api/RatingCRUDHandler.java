package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.Rating;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.RatingService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Optional;

public class RatingCRUDHandler extends BaseHandler {

    private final RatingService ratingService;
    private final RatingActionHandler actionHandler;

    public RatingCRUDHandler(RatingService ratingService, AuthService authService) {
        super(authService);
        this.ratingService = ratingService;
        this.actionHandler = new RatingActionHandler(ratingService, authService);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (isOptionsRequest(ex)) return;

        String path = ex.getRequestURI().getPath();

        if (path.endsWith("/like") || path.endsWith("/likes") || path.endsWith("/confirm")) {
            actionHandler.handle(ex);
            return;
        }

        String method = ex.getRequestMethod();

        try {
            if (method.equals("GET") && path.matches("/api/ratings/average/\\d+")) {
                handleGetAverage(ex);
                return;
            }
            if (method.equals("PUT") && path.matches("/api/ratings/\\d+")) {
                handleUpdate(ex);
                return;
            }
            if (method.equals("DELETE") && path.matches("/api/ratings/\\d+")) {
                handleDelete(ex);
                return;
            }
            send(ex, 404, "{\"error\":\"Unknown endpoint\"}");

        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Server error\"}");
        }
    }

    private void handleGetAverage(HttpExchange ex) throws IOException {
        int mediaId = extractId(ex.getRequestURI().getPath());
        double avg = ratingService.getAverageRating(mediaId);
        send(ex, 200, String.valueOf(avg));
    }

    private void handleUpdate(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int ratingId = extractId(ex.getRequestURI().getPath());
        Rating r = mapper.readValue(ex.getRequestBody(), Rating.class);
        r.setId(ratingId);
        try {
            Rating updated = ratingService.updateRating(r, user.get().getId());
            send(ex, 200, mapper.writeValueAsString(updated));
        } catch (SecurityException se) {
            send(ex, 403, "{\"error\":\"Not your rating\"}");
        } catch (IllegalArgumentException iae) {
            send(ex, 404, "{\"error\":\"Rating not found\"}");
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        int ratingId = extractId(ex.getRequestURI().getPath());
        try {
            ratingService.deleteRating(ratingId, user.get().getId());
            send(ex, 204);
        } catch (SecurityException se) {
            send(ex, 403, "{\"error\":\"Not your rating\"}");
        } catch (IllegalArgumentException iae) {
            send(ex, 404, "{\"error\":\"Rating not found\"}");
        }
    }
}