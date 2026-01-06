package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.interfaces.RatingRepositoryIF;
import at.technikum_wien.mrp.model.Rating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepositoryIF ratingRepo;
    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepo);
    }

    @Test
    void testAddRating_ShouldBeInvisibleInitially() {
        Rating inputRating = new Rating();
        inputRating.setStars(5);
        inputRating.setComment("Super Film!");

        when(ratingRepo.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Rating result = ratingService.addRating(inputRating, 1);

        assertFalse(result.isVisible(), "Ein neues Rating muss standardmäßig unsichtbar sein (visible=false)");

        assertEquals(1, result.getUserId(), "Die User-ID muss korrekt gesetzt werden");
        verify(ratingRepo).save(any(Rating.class));
    }

    // TEST 2: Bestätigen funktioniert
    @Test
    void testConfirmRating_ShouldMakeRatingVisible() {
        Rating existingRating = new Rating();
        existingRating.setId(10);
        existingRating.setUserId(99);
        existingRating.setVisible(false);

        when(ratingRepo.findById(10)).thenReturn(java.util.Optional.of(existingRating));

        ratingService.confirmRating(10, 99);

        assertTrue(existingRating.isVisible(), "Nach Confirm muss das Rating sichtbar sein");
        verify(ratingRepo).save(existingRating);
    }

    // TEST 3: Sicherheits-Check (Fremder User darf nicht bestätigen)
    @Test
    void testConfirmRating_WrongUser_ShouldThrowException() {
        Rating existingRating = new Rating();
        existingRating.setId(10);
        existingRating.setUserId(99);

        when(ratingRepo.findById(10)).thenReturn(java.util.Optional.of(existingRating));
        assertThrows(SecurityException.class, () -> {
            ratingService.confirmRating(10, 50);
        });

        verify(ratingRepo, org.mockito.Mockito.never()).save(any());
    }
    // TEST 4: Durchschnittsberechnung
    @Test
    void testGetAverageRating_ShouldCalculateCorrectly() {
        Rating r1 = new Rating(); r1.setStars(2);
        Rating r2 = new Rating(); r2.setStars(4);

        when(ratingRepo.findByMediaId(5)).thenReturn(java.util.List.of(r1, r2));

        double average = ratingService.getAverageRating(5);

        assertEquals(3.0, average, 0.001, "Durchschnitt von 2 und 4 muss 3.0 sein");
    }

    // TEST 5: Durchschnitt bei 0 Bewertungen (Edge Case)
    @Test
    void testGetAverageRating_EmptyList_ShouldReturnZero() {
        when(ratingRepo.findByMediaId(99)).thenReturn(java.util.List.of());

        double average = ratingService.getAverageRating(99);

        assertEquals(0.0, average, "Wenn keine Ratings da sind, muss der Durchschnitt 0.0 sein");
    }

    // TEST 6: Leaderboard
    @Test
    void testGetLeaderboard_ShouldReturnList() {
        at.technikum_wien.mrp.model.LeaderboardEntry e1 = new at.technikum_wien.mrp.model.LeaderboardEntry("SuperUser", 100);
        at.technikum_wien.mrp.model.LeaderboardEntry e2 = new at.technikum_wien.mrp.model.LeaderboardEntry("Newbie", 1);

        when(ratingRepo.getLeaderboard()).thenReturn(java.util.List.of(e1, e2));

        java.util.List<at.technikum_wien.mrp.model.LeaderboardEntry> result = ratingService.getLeaderboard();

        assertEquals(2, result.size());
        assertEquals("SuperUser", result.get(0).getUsername());
        assertEquals(100, result.get(0).getRatingCount());

        verify(ratingRepo).getLeaderboard();
    }
}