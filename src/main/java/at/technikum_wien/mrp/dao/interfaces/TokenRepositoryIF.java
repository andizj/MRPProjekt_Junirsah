package at.technikum_wien.mrp.dao.interfaces;

import at.technikum_wien.mrp.model.Token;
import java.util.Optional;

public interface TokenRepositoryIF {
    void save(Token token);
    Optional<Token> findByToken(String tokenValue);
    void delete(String tokenValue);
}
