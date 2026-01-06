package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.UserService;
import java.util.Optional;

public class UserRequestHelper {

    private final UserService userService;

    public UserRequestHelper(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> resolveTargetUser(int userId) {
        return userService.findById(userId);
    }

    public boolean isSelf(User requester, User target) {
        return requester.getId() == target.getId();
    }

    public int extractId(String path, String suffix) {
        String idStr = path.replace("/api/users/", "").replace(suffix, "");
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid User ID in URL");
        }
    }
}