package at.technikum_wien.mrp.api;

import at.technikum_wien.mrp.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseHandlerTest {

    @Mock
    private AuthService authService;

    private static class TestHandler extends BaseHandler {
        public TestHandler(AuthService authService) {
            super(authService);
        }
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) {}
    }

    private TestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestHandler(authService);
    }

    @Test
    void testExtractId_ValidPath_ShouldReturnId() {
        int id = handler.extractId("/api/media/123");
        assertEquals(123, id);
    }

    @Test
    void testExtractId_WithSuffix_ShouldReturnId() {
        int id = handler.extractId("/api/ratings/55/like", "/like");
        assertEquals(55, id);
    }

    @Test
    void testExtractId_InvalidId_ShouldReturnMinusOne() {
        int id = handler.extractId("/api/media/abc");
        assertEquals(-1, id, "Bei ungültiger ID sollte -1 (oder Fehlercode) kommen");
    }

    // TEST 21: Query Parameter parsen
    @Test
    void testGetQueryMap_ComplexQuery_ShouldParseMap() {
        String query = "search=Matrix&year=1999&genre=Sci-Fi";

        Map<String, String> params = handler.getQueryMap(query);

        assertEquals(3, params.size());
        assertEquals("Matrix", params.get("search"));
        assertEquals("1999", params.get("year"));
        assertEquals("Sci-Fi", params.get("genre"));
    }
}