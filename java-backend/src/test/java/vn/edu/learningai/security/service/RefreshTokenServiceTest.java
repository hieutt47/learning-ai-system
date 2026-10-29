package vn.edu.learningai.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.learningai.entity.AuthSession;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.AuthSessionRepository;
import vn.edu.learningai.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    private final AuthSessionRepository sessions = mock(AuthSessionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(sessions, users);
    private final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "refreshTTL", 604800000L);
    }

    @Test
    void createStoresOnlyHashAndReturnsSessionMetadata() throws Exception {
        User user = User.builder().id(1L).isActive(true).build();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(sessions.save(any(AuthSession.class))).thenAnswer(invocation -> {
            AuthSession session = invocation.getArgument(0);
            session.setId(10L);
            return session;
        });
        var result = service.createRefreshToken(1L);
        var captor = org.mockito.ArgumentCaptor.forClass(AuthSession.class);
        verify(sessions).save(captor.capture());
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(result.refreshToken().getBytes(StandardCharsets.US_ASCII)));
        assertEquals(expectedHash, captor.getValue().getRefreshTokenHash());
        assertEquals(32, Base64.getUrlDecoder().decode(result.refreshToken()).length);
        assertEquals(1L, result.userId());
        assertEquals(10L, result.sessionId());
        assertEquals(captor.getValue().getExpiresAt(), result.expiresAt());
    }

    @Test
    void missingOrMalformedRefreshReturnsUnauthorizedBeforeDatabaseLookup() {
        for (String invalid : new String[]{null, "", "not-a-token"}) {
            ApiException error = assertThrows(ApiException.class, () -> service.rotateRefreshToken(invalid));
            assertEquals(ErrorCode.UNAUTHORIZED, error.getErrorCode());
        }
        verifyNoInteractions(sessions);
    }

    @Test
    void oldOrUnknownTokenReturnsUnauthorized() {
        when(sessions.findByRefreshTokenHash(anyString())).thenReturn(Optional.empty());
        ApiException error = assertThrows(ApiException.class, () -> service.rotateRefreshToken(token));
        assertEquals(ErrorCode.UNAUTHORIZED, error.getErrorCode());
    }

    @Test
    void rotateChangesTokenAndPreservesAbsoluteExpiry() {
        AuthSession session = activeSession();
        Instant expiresAt = session.getExpiresAt();
        var result = service.rotateRefreshToken(token);
        assertNotEquals(token, result.refreshToken());
        assertEquals(expiresAt, result.expiresAt());
        assertEquals(1L, result.userId());
        assertEquals(10L, result.sessionId());
        assertNull(session.getRevokedAt());
    }

    @Test
    void revokedExpiredAndInactiveSessionsCannotRefresh() {
        AuthSession session = activeSession();
        session.setRevokedAt(Instant.now());
        assertThrows(ApiException.class, () -> service.rotateRefreshToken(token));
        session.setRevokedAt(null);
        session.setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(ApiException.class, () -> service.rotateRefreshToken(token));
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        session.getUser().setIsActive(false);
        assertThrows(ApiException.class, () -> service.rotateRefreshToken(token));
    }

    @Test
    void logoutIsIdempotentAndKeepsFirstRevocationTime() {
        service.revokeRefreshToken(null);
        service.revokeRefreshToken("");
        verifyNoInteractions(sessions);
        when(sessions.findByRefreshTokenHash(anyString())).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.revokeRefreshToken(token));
        AuthSession session = activeSession();
        service.revokeRefreshToken(token);
        Instant revokedAt = session.getRevokedAt();
        service.revokeRefreshToken(token);
        assertNotNull(revokedAt);
        assertEquals(revokedAt, session.getRevokedAt());
    }

    private AuthSession activeSession() {
        AuthSession session = AuthSession.builder().id(10L)
            .user(User.builder().id(1L).isActive(true).build())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(sessions.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(session));
        return session;
    }
}
