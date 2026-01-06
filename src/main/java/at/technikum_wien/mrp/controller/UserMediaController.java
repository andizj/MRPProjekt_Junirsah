package at.technikum_wien.mrp.controller;

import at.technikum_wien.mrp.api.BaseHandler;
import at.technikum_wien.mrp.api.UserRequestHelper;
import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.MediaService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UserMediaController extends BaseHandler {

    private final MediaService mediaService;
    private final UserRequestHelper helper;

    public UserMediaController(MediaService mediaService, UserRequestHelper helper, AuthService authService) {
        super(authService);
        this.mediaService = mediaService;
        this.helper = helper;
    }

    public void handleGetFavorites(HttpExchange ex, User targetUser) throws IOException {
        List<MediaEntry> favorites = mediaService.getFavorites(targetUser.getId());
        send(ex, 200, mapper.writeValueAsString(favorites));
    }

    public void handleGetRecommendations(HttpExchange ex, User requester, User targetUser) throws IOException {
        if (!helper.isSelf(requester, targetUser)) {
            send(ex, 403, "{\"error\":\"Access denied\"}");
            return;
        }

        List<MediaEntry> recs = mediaService.getRecommendations(targetUser.getId());
        send(ex, 200, mapper.writeValueAsString(recs));
    }
    @Override
    public void handle(HttpExchange ex) {
        throw new UnsupportedOperationException("This controller is only called via specific methods.");
    }
}