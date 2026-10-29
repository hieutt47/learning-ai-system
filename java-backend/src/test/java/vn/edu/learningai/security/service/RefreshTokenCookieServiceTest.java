package vn.edu.learningai.security.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenCookieServiceTest {
    @Test
    void cookieUsesRemainingSessionLifetimeAndExpiredSessionClearsCookie() {
        RefreshTokenCookieService service = new RefreshTokenCookieService();
        ReflectionTestUtils.setField(service, "secure", true);
        ReflectionTestUtils.setField(service, "sameSite", "Lax");
        var cookie = service.create("token", Instant.now().plusSeconds(60));
        assertTrue(cookie.getMaxAge().getSeconds() > 0);
        assertTrue(cookie.getMaxAge().getSeconds() <= 60);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals(0, service.create("token", Instant.now().minusSeconds(1)).getMaxAge().getSeconds());
        assertEquals(0, service.clear().getMaxAge().getSeconds());
    }
}
