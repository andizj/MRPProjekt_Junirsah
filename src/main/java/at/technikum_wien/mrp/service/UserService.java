package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.UserRepository;
import at.technikum_wien.mrp.dao.UserRepositoryIF;
import at.technikum_wien.mrp.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {

    private final UserRepositoryIF userRepo;

    public UserService(UserRepositoryIF userRepo) {
        this.userRepo = userRepo;
    }

    public User register(String username, String plainPassword) {
        if (username == null || plainPassword == null ||
                username.isBlank() || plainPassword.isBlank()) {
            throw new IllegalArgumentException("username or password empty");
        }

        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists");
        }

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User newUser = new User(0, username, hashed, null);
        return userRepo.save(newUser);
    }
}
