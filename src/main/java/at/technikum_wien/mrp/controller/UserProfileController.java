package at.technikum_wien.mrp.controller;

import at.technikum_wien.mrp.api.BaseHandler;
import at.technikum_wien.mrp.api.UserRequestHelper;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.model.UserProfileStats;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class UserProfileController extends BaseHandler {

    private final UserService userService;
    private final UserRequestHelper helper;

    public UserProfileController(UserService userService, UserRequestHelper helper, AuthService authService) {
        super(authService);
        this.userService = userService;
        this.helper = helper;
    }

    public void handleGetProfile(HttpExchange ex, User targetUser) throws IOException {
        UserProfileStats stats = userService.getUserProfile(targetUser.getId());
        send(ex, 200, mapper.writeValueAsString(stats));
    }

    public void handleUpdateProfile(HttpExchange ex, User requester, User targetUser) throws IOException {
        if (!helper.isSelf(requester, targetUser)) {
            send(ex, 403, "{\"error\":\"You can only update your own profile\"}");
            return;
        }

        try {
            User req = mapper.readValue(ex.getRequestBody(), User.class);
            userService.updateUser(targetUser.getUsername(), req.getUsername(), req.getEmail(), req.getFavoriteGenre());
            send(ex, 200, "{\"message\":\"Profile updated\"}");
        } catch (Exception e) {
            e.printStackTrace(); // oder Logger
            send(ex, 500, "{\"error\":\"Internal error\"}");
        }
    }
    @Override
    public void handle(HttpExchange ex) {
        throw new UnsupportedOperationException("This controller is only called via specific methods.");
    }
}