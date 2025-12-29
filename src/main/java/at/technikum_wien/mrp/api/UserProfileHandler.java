package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.model.UserProfileStats;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Optional;

public class UserProfileHandler extends BaseHandler {

    private final UserService userService;

    public UserProfileHandler(UserService userService, AuthService authService) {
        super(authService);
        this.userService = userService;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (isOptionsRequest(ex)) return;

        String path = ex.getRequestURI().getPath();
        // Erwartet: GET /api/users/{username}/profile
        if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && path.matches("/api/users/[^/]+/profile")) {
            handleGetProfile(ex);
            return;
        }

        send(ex, 404, "{\"error\":\"Not found\"}");
    }

    private void handleGetProfile(HttpExchange ex) throws IOException {
        Optional<User> requester = getUser(ex);
        if (requester.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        String path = ex.getRequestURI().getPath();
        String username = path.replace("/api/users/", "").replace("/profile", "");

        try {
            UserProfileStats stats = userService.getUserProfile(username);
            send(ex, 200, mapper.writeValueAsString(stats));
        } catch (IllegalArgumentException e) {
            send(ex, 404, "{\"error\":\"User not found\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Internal error\"}");
        }
    }
}