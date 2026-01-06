package at.technikum_wien.mrp;

import at.technikum_wien.mrp.api.*;
import at.technikum_wien.mrp.dao.impl.*;
import at.technikum_wien.mrp.dao.interfaces.*;
import at.technikum_wien.mrp.database.DatabaseConnection;
import at.technikum_wien.mrp.database.DatabaseConnectionIF;
import at.technikum_wien.mrp.service.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws IOException {

        // DB Provider
        DatabaseConnectionIF dbProvider = new DatabaseConnection();

        // Repositories
        UserRepositoryIF userRepo = new UserRepository(dbProvider);
        TokenRepositoryIF tokenRepo = new TokenRepository();
        MediaRepositoryIF mediaRepo = new MediaRepository(dbProvider);
        RatingRepositoryIF ratingRepo = new RatingRepository(dbProvider);
        FavoriteRepositoryIF favoriteRepo = new FavoriteRepository(dbProvider);

        // Services
        UserService userService = new UserService(userRepo, ratingRepo, mediaRepo);
        AuthService authService = new AuthService(userRepo, tokenRepo);
        MediaService mediaService = new MediaService(mediaRepo, favoriteRepo, ratingRepo);
        RatingService ratingService = new RatingService(ratingRepo);


        // HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // USER
        server.createContext("/api/users/register", new UserRegisterHandler(authService));
        server.createContext("/api/users/login", new UserLoginHandler(authService));
        server.createContext("/api/users/", new UserRequestDispatcher(userService, ratingService, mediaService, authService));

        // MEDIA
        server.createContext("/api/media", new MediaHandler(authService, mediaService, ratingService));
        server.createContext("/api/media/", new MediaHandler(authService, mediaService, ratingService));

        // RATINGS
        server.createContext("/api/ratings", new RatingCRUDHandler(ratingService, authService));
        server.createContext("/api/ratings/", new RatingCRUDHandler(ratingService, authService));

        // FAVORTIES
        server.createContext("/api/favorites", new FavoriteHandler(mediaService, authService));

        // LEADERBOARD
        server.createContext("/api/leaderboard", new LeaderboardHandler(ratingService, authService));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("✅ Server running on http://localhost:8080");
    }
}
