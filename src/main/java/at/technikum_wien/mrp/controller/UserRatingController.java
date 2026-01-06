package at.technikum_wien.mrp.controller;

import at.technikum_wien.mrp.api.BaseHandler;
import at.technikum_wien.mrp.api.UserRequestHelper;
import at.technikum_wien.mrp.model.Rating;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.RatingService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UserRatingController extends BaseHandler {

    private final RatingService ratingService;
    private final UserRequestHelper helper;

    public UserRatingController(RatingService ratingService, UserRequestHelper helper, AuthService authService) {
        super(authService);
        this.ratingService = ratingService;
        this.helper = helper;
    }

    public void handleGetHistory(HttpExchange ex, User targetUser) throws IOException {
        List<Rating> history = ratingService.getRatingsByUser(targetUser.getId());
        send(ex, 200, mapper.writeValueAsString(history));
    }
    @Override
    public void handle(HttpExchange ex) {
        throw new UnsupportedOperationException("This controller is only called via specific methods.");
    }
}