package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.controller.UserMediaController;
import at.technikum_wien.mrp.controller.UserProfileController;
import at.technikum_wien.mrp.controller.UserRatingController;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.MediaService;
import at.technikum_wien.mrp.service.RatingService;
import at.technikum_wien.mrp.service.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Optional;

public class UserRequestDispatcher extends BaseHandler {

    private final UserRequestHelper helper;
    private final UserProfileController profileController;
    private final UserMediaController mediaController;
    private final UserRatingController ratingController;

    public UserRequestDispatcher(UserService userService, RatingService ratingService, MediaService mediaService, AuthService authService) {
        super(authService);
        this.helper = new UserRequestHelper(userService);
        this.profileController = new UserProfileController(userService, helper, authService);
        this.mediaController = new UserMediaController(mediaService, helper, authService);
        this.ratingController = new UserRatingController(ratingService, helper, authService);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (isOptionsRequest(ex)) return;

        Optional<User> requester = getUser(ex);
        if (requester.isEmpty()) {
            send(ex, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if (path.matches("/api/users/\\d+/profile")) {
                int userId = helper.extractId(path, "/profile");
                Optional<User> targetUser = helper.resolveTargetUser(userId);

                if (targetUser.isEmpty()) {
                    send(ex, 404, "{\"error\":\"User not found\"}");
                    return;
                }

                if ("GET".equalsIgnoreCase(method)) {
                    profileController.handleGetProfile(ex, targetUser.get());
                    return;
                }
                if ("PUT".equalsIgnoreCase(method)) {
                    profileController.handleUpdateProfile(ex, requester.get(), targetUser.get());
                    return;
                }
            }

            else if (path.matches("/api/users/\\d+/recommendations")) {
                int userId = helper.extractId(path, "/recommendations");
                Optional<User> targetUser = helper.resolveTargetUser(userId);

                if (targetUser.isEmpty()) {
                    send(ex, 404, "{\"error\":\"User not found\"}");
                    return;
                }

                if ("GET".equalsIgnoreCase(method)) {
                    mediaController.handleGetRecommendations(ex, requester.get(), targetUser.get());
                    return;
                }
            }

            else if (path.matches("/api/users/\\d+/favorites")) {
                int userId = helper.extractId(path, "/favorites");

                Optional<User> targetUser = helper.resolveTargetUser(userId);
                if (targetUser.isEmpty()) {
                    send(ex, 404, "{\"error\":\"User not found\"}");
                    return;
                }

                if ("GET".equalsIgnoreCase(method)) {
                    mediaController.handleGetFavorites(ex, targetUser.get());
                    return;
                }
            }

            else if (path.matches("/api/users/\\d+/ratings")) {
                int userId = helper.extractId(path, "/ratings");

                Optional<User> targetUser = helper.resolveTargetUser(userId);
                if (targetUser.isEmpty()) {
                    send(ex, 404, "{\"error\":\"User not found\"}");
                    return;
                }

                if ("GET".equalsIgnoreCase(method)) {
                    ratingController.handleGetHistory(ex, targetUser.get());
                    return;
                }
            }

            send(ex, 404, "{\"error\":\"Not found\"}");

        } catch (NumberFormatException e) {
            send(ex, 400, "{\"error\":\"Invalid ID format\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\"Internal error\"}");
        }
    }
}