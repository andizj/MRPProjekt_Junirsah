package at.technikum_wien.mrp;

import at.technikum_wien.mrp.api.*;
import at.technikum_wien.mrp.dao.*;
import at.technikum_wien.mrp.service.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws IOException {

        // DB Provider
        DatabaseConnection dbProvider = new DatabaseConnection();

        // Repositories
        UserRepositoryIF userRepo = new UserRepository(dbProvider);
        TokenRepositoryIF tokenRepo = new TokenRepository();
        MediaRepositoryIF mediaRepo = new MediaRepository(dbProvider);
        RatingRepositoryIF ratingRepo = new RatingRepository(dbProvider);
        FavoriteRepositoryIF favoriteRepo = new FavoriteRepository(dbProvider);

        // Services
        UserService userService = new UserService(userRepo);
        AuthService authService = new AuthService(userRepo, tokenRepo);
        MediaService mediaService = new MediaService(mediaRepo, favoriteRepo);
        RatingService ratingService = new RatingService(ratingRepo);

        // HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // USER
        server.createContext("/api/users/register", new UserRegisterHandler(userService));
        server.createContext("/api/users/login", new UserLoginHandler(authService));

        // MEDIA
        server.createContext("/api/media", new MediaHandler(authService, mediaService));
        server.createContext("/api/media/", new MediaHandler(authService, mediaService)); // wichtig für /api/media/{id}

        // RATINGS
        //server.createContext("/api/media/", new RatingHandler(ratingService, authService)); // wichtig für /api/media/{id}/rate
        server.createContext("/api/ratings", new RatingHandler(ratingService, authService));
        server.createContext("/api/ratings/", new RatingHandler(ratingService, authService));

        // FAVORTIES
        server.createContext("/api/favorites", new FavoriteHandler(mediaService, authService));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("✅ Server running on http://localhost:8080");
    }
}
