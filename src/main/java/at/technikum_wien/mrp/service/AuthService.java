package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.interfaces.TokenRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.UserRepositoryIF;
import at.technikum_wien.mrp.model.Token;
import at.technikum_wien.mrp.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class AuthService {

    private final UserRepositoryIF userRepo;
    private final TokenRepositoryIF tokenRepo;

    public AuthService(UserRepositoryIF userRepo, TokenRepositoryIF tokenRepo) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
    }

    public User register(String username, String plainPassword) {
        if (userRepo.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken.");
        }

        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(hashed);

        return userRepo.save(newUser);
    }

    public String login(String username, String plainPassword) {
        Optional<User> opt = userRepo.findByUsername(username);
        if (opt.isEmpty()) throw new IllegalArgumentException("username or password wrong");

        User user = opt.get();
        if (!BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("username or password wrong");
        }

        String tokenValue = UUID.randomUUID().toString();
        tokenRepo.save(new Token(tokenValue, user.getId(), LocalDateTime.now()));
        return tokenValue;
    }

    public Optional<User> validateToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) return Optional.empty();
        Optional<Token> t = tokenRepo.findByToken(tokenValue);
        if (t.isEmpty()) return Optional.empty();
        return userRepo.findById(t.get().getUserId());
    }

    public void logout(String tokenValue) {
        tokenRepo.delete(tokenValue);
    }
}