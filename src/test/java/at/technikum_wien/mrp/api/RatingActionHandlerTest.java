package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.model.User;
import at.technikum_wien.mrp.service.AuthService;
import at.technikum_wien.mrp.service.RatingService;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingActionHandlerTest {

    @Mock private RatingService ratingService;
    @Mock private AuthService authService;
    @Mock private HttpExchange exchange;

    private RatingActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RatingActionHandler(ratingService, authService);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(mock(OutputStream.class));
    }

    // TEST 22: Dispatching zum "Like" Service
    @Test
    void testHandle_LikePath_ShouldCallLikeService() throws Exception {
        User user = new User(); user.setId(99);
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        exchange.getRequestHeaders().add("Authorization", "Bearer token123");
        when(authService.validateToken("token123")).thenReturn(Optional.of(user));

        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(new URI("/api/ratings/10/like"));

        handler.handle(exchange);

        verify(ratingService).likeRating(10, 99);
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
    }
}