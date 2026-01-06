package at.technikum_wien.mrp.service;

import at.technikum_wien.mrp.dao.interfaces.TokenRepositoryIF;
import at.technikum_wien.mrp.dao.interfaces.UserRepositoryIF;
import at.technikum_wien.mrp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryIF userRepo;

    @Mock
    private TokenRepositoryIF tokenRepo;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo, tokenRepo);
    }

    // TEST 6: Erfolgreicher Login
    @Test
    void testLogin_Success_ShouldReturnToken() {
        User dbUser = new User();
        dbUser.setUsername("Andi");
        dbUser.setPasswordHash(BCrypt.hashpw("secret123", BCrypt.gensalt()));

        when(userRepo.findByUsername("Andi")).thenReturn(Optional.of(dbUser));

        String token = authService.login("Andi", "secret123");

        assertNotNull(token, "Login sollte einen Token zurückgeben");
        assertFalse(token.isBlank(), "Token darf nicht leer sein");

        verify(tokenRepo).save(any());
    }

    // TEST 7: Login mit falschem Passwort
    @Test
    void testLogin_WrongPassword_ShouldThrowException() {
        User dbUser = new User();
        dbUser.setUsername("Andi");
        dbUser.setPasswordHash(BCrypt.hashpw("richtigesPasswort", BCrypt.gensalt()));

        when(userRepo.findByUsername("Andi")).thenReturn(Optional.of(dbUser));

        assertThrows(IllegalArgumentException.class, () -> {
            authService.login("Andi", "falschesPasswort");
        });

        verify(tokenRepo, never()).save(any());
    }

    // TEST 8: Registrierung (Erfolg)
    @Test
    void testRegister_NewUser_ShouldSaveUser() {
        when(userRepo.existsByUsername("NewUser")).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User created = authService.register("NewUser", "password");

        assertNotNull(created);
        assertEquals("NewUser", created.getUsername());
        assertNotEquals("password", created.getPasswordHash());
        verify(userRepo).save(any(User.class));
    }

    // TEST 9: Registrierung (User existiert schon)
    @Test
    void testRegister_ExistingUser_ShouldThrowException() {
        when(userRepo.existsByUsername("OldUser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            authService.register("OldUser", "pw");
        });

        verify(userRepo, never()).save(any());
    }
}