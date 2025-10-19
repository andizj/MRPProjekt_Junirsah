package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.User;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UserRepository implements UserRepositoryIF {

    private final Map<Integer, User> users = new HashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Override
    public User save(User user) {
        int id = idCounter.getAndIncrement();
        user.setId(id);
        users.put(id, user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findById(int id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
}
