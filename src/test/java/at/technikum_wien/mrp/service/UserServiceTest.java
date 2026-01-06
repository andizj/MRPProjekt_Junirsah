package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.interfaces.MediaRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.RatingRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.UserRepositoryIF;
import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.Rating;
import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.model.UserProfileStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepositoryIF userRepo;
    @Mock private RatingRepositoryIF ratingRepo;
    @Mock private MediaRepositoryIF mediaRepo;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepo, ratingRepo, mediaRepo);
    }

    @Test
    void testGetUserProfile_ShouldReturnCorrectStats() {
        User user = new User();
        user.setId(1);
        user.setUsername("TestUser");

        when(userRepo.findByUsername("TestUser")).thenReturn(Optional.of(user));
        when(ratingRepo.countRatingsByUserId(1)).thenReturn(10);
        when(ratingRepo.getAverageRatingByUserId(1)).thenReturn(4.5);
        when(ratingRepo.findByUserId(1)).thenReturn(List.of());

        UserProfileStats stats = userService.getUserProfile("TestUser");

        assertEquals("TestUser", stats.getUsername());
        assertEquals(10, stats.getRatingCount());
        assertEquals(4.5, stats.getAverageScore());
    }

    // TEST 16: Lieblings-Genre Berechnung
    @Test
    void testGetUserProfile_ShouldCalculateFavoriteGenre() {
        User user = new User(); user.setId(1); user.setUsername("Fan");

        Rating r1 = new Rating(); r1.setMediaId(10);
        Rating r2 = new Rating(); r2.setMediaId(20);

        MediaEntry m1 = new MediaEntry(); m1.setGenres(new String[]{"Action"});
        MediaEntry m2 = new MediaEntry(); m2.setGenres(new String[]{"Action", "Drama"});

        when(userRepo.findByUsername("Fan")).thenReturn(Optional.of(user));
        when(ratingRepo.findByUserId(1)).thenReturn(List.of(r1, r2));
        when(mediaRepo.findById(10)).thenReturn(Optional.of(m1));
        when(mediaRepo.findById(20)).thenReturn(Optional.of(m2));

        UserProfileStats stats = userService.getUserProfile("Fan");
        assertEquals("Action", stats.getFavoriteGenre());
    }
}