package at.technikum_wien.mrp.dao;

import at.technikum_wien.mrp.model.Token;
import java.util.*;

public class TokenRepository implements TokenRepositoryIF {

    private final Map<String, Token> tokens = new HashMap<>();

    @Override
    public void save(Token token) {
        tokens.put(token.getToken(), token);
    }

    @Override
    public Optional<Token> findByToken(String tokenValue) {
        return Optional.ofNullable(tokens.get(tokenValue));
    }

    @Override
    public void delete(String tokenValue) {
        tokens.remove(tokenValue);
    }
}
