package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.User;
import java.util.Optional;

public interface UserRepositoryIF {
    User save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findById(int id);
    boolean existsByUsername(String username);
}
