package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.interfaces.FavoriteRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.MediaRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.RatingRepositoryIF;
import at.technikum_wien.mrp.model.MediaEntry;
import at.technikum_wien.mrp.model.Rating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepositoryIF mediaRepo;
    @Mock
    private FavoriteRepositoryIF favoriteRepo;
    @Mock
    private RatingRepositoryIF ratingRepo;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(mediaRepo, favoriteRepo, ratingRepo);
    }

    // TEST 10: Media erstellen (Erfolg)
    @Test
    void testCreate_ValidMedia_ShouldSave() {
        MediaEntry entry = new MediaEntry();
        entry.setTitle("Test Movie");

        entry.setReleaseYear(2020);
        entry.setMediaType("MOVIE");
        entry.setGenres(new String[]{"Action"});
        entry.setAgeRestriction(12);

        when(mediaRepo.save(any())).thenReturn(entry);

        MediaEntry result = mediaService.create(entry);
        assertNotNull(result);
        verify(mediaRepo).save(entry);
    }

    // TEST 11: Media ohne Titel (Fehler)
    @Test
    void testCreate_NoTitle_ShouldThrowException() {
        MediaEntry entry = new MediaEntry(); // Titel ist null

        assertThrows(IllegalArgumentException.class, () -> {
            mediaService.create(entry);
        });
        verify(mediaRepo, never()).save(any());
    }

    // TEST 12: Update durch falschen User (Security)
    @Test
    void testUpdate_WrongUser_ShouldThrowSecurityException() {
        MediaEntry existing = new MediaEntry();
        existing.setId(1);
        existing.setCreatorId(10);

        MediaEntry updateData = new MediaEntry();
        updateData.setId(1);

        when(mediaRepo.findById(1)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class, () -> {
            mediaService.update(updateData, 99);
        });
    }

    // TEST 13: Delete durch falschen User (Security)
    @Test
    void testDelete_WrongUser_ShouldThrowSecurityException() {
        MediaEntry existing = new MediaEntry();
        existing.setId(1);
        existing.setCreatorId(10);

        when(mediaRepo.findById(1)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class, () -> {
            mediaService.delete(1, 99);
        });
        verify(mediaRepo, never()).delete(anyInt());
    }

    // TEST 14: Recommendations (Logik-Check)
    @Test
    void testGetRecommendations_ShouldFilterAlreadyRated() {
        Rating r1 = new Rating();
        r1.setMediaId(1);
        r1.setStars(5);

        MediaEntry m1 = new MediaEntry();
        m1.setId(1);
        m1.setGenres(new String[]{"Sci-Fi"});

        MediaEntry m2 = new MediaEntry();
        m2.setId(2);
        m2.setTitle("Recommended Movie");
        m2.setGenres(new String[]{"Sci-Fi"});

        when(ratingRepo.findByUserId(100)).thenReturn(List.of(r1));
        when(mediaRepo.findById(1)).thenReturn(Optional.of(m1));

        when(mediaRepo.findByGenres(any())).thenReturn(List.of(m1, m2));

        List<MediaEntry> recs = mediaService.getRecommendations(100);

        assertEquals(1, recs.size(), "Es sollte genau 1 Film empfohlen werden");
        assertEquals(2, recs.get(0).getId(), "M1 sollte gefiltert werden (schon gesehen), M2 empfohlen");
    }
}