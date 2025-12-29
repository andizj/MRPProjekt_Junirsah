package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.RatingService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Optional;

public class RatingActionHandler extends BaseHandler {

    private final RatingService ratingService;

    public RatingActionHandler(RatingService ratingService, AuthService authService) {
        super(authService);
        this.ratingService = ratingService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // Hier kein isOptionsRequest nötig, das macht der CRUDHandler schon beim Dispatching
        // oder man lässt es sicherheitshalber drin.
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        try {
            if (method.equals("POST") && path.endsWith("/like")) {
                handleLike(ex);
                return;
            }
            if (method.equals("DELETE") && path.endsWith("/like")) {
                handleUnlike(ex);
                return;
            }
            if (method.equals("GET") && path.endsWith("/likes")) {
                handleGetLikeCount(ex);
                return;
            }
            if (method.equals("PUT") && path.endsWith("/confirm")) {
                handleConfirm(ex);
                return;
            }
            send(ex, 404, "{\"error\":\"Unknown action endpoint\"}");

        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Server error\"}");
        }
    }

    private void handleLike(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) { send(ex, 401, "{\"error\":\"Unauthorized\"}"); return; }

        int ratingId = extractId(ex.getRequestURI().getPath(), "/like");
        try {
            ratingService.likeRating(ratingId, user.get().getId());
            send(ex, 200, "{\"message\":\"Liked\"}");
        } catch (IllegalArgumentException e) {
            send(ex, 404, "{\"error\":\"Rating not found or hidden\"}");
        }
    }

    private void handleUnlike(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) { send(ex, 401, "{\"error\":\"Unauthorized\"}"); return; }

        int ratingId = extractId(ex.getRequestURI().getPath(), "/like");
        ratingService.unlikeRating(ratingId, user.get().getId());
        send(ex, 200, "{\"message\":\"Unliked\"}");
    }

    private void handleGetLikeCount(HttpExchange ex) throws IOException {
        int ratingId = extractId(ex.getRequestURI().getPath(), "/likes");
        int count = ratingService.getLikeCount(ratingId);
        send(ex, 200, "{\"count\":" + count + "}");
    }

    private void handleConfirm(HttpExchange ex) throws IOException {
        Optional<User> user = getUser(ex);
        if (user.isEmpty()) { send(ex, 401, "{\"error\":\"Unauthorized\"}"); return; }

        int ratingId = extractId(ex.getRequestURI().getPath(), "/confirm");
        try {
            ratingService.confirmRating(ratingId, user.get().getId());
            send(ex, 200, "{\"message\":\"Rating confirmed\"}");
        } catch (Exception e) {
            send(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}