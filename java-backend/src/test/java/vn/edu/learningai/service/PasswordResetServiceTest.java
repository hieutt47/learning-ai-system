package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.learningai.dto.request.ResetPasswordRequest;
import vn.edu.learningai.entity.PasswordResetToken;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.PasswordResetTokenRepository;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.service.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock private PasswordResetTokenRepository tokens;
    @Mock private UserRepository users;
    @Mock(mockMaker = MockMakers.SUBCLASS) private RefreshTokenService refreshTokens;
    @Mock(mockMaker = MockMakers.SUBCLASS) private EmailService mail;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private PasswordResetService service;
    private User user;
    private ResetPasswordRequest request;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(tokens, users, passwordEncoder, refreshTokens, mail);
        user = User.builder().id(7L).email("student@example.test")
            .emailVerifiedAt(Instant.now()).passwordHash(passwordEncoder.encode("CurrentPass123!")).build();
        request = new ResetPasswordRequest();
        request.setToken("A".repeat(43));
        request.setNewPassword("NewPassword123!");
    }

    @Test
    void requestingResetInvalidatesOldLinksStoresHashAndSendsRawToken() throws Exception {
        when(users.findForPasswordResetByEmail(user.getEmail())).thenReturn(Optional.of(user));
        String originalHash = user.getPasswordHash();
        Instant before = Instant.now();

        service.forgotPassword(" STUDENT@EXAMPLE.TEST ");

        ArgumentCaptor<PasswordResetToken> record = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(users, tokens, mail);
        order.verify(users).findForPasswordResetByEmail(user.getEmail());
        order.verify(tokens).invalidateAllForUser(eq(user.getId()), any(Instant.class));
        order.verify(tokens).save(record.capture());
        order.verify(mail).sendPasswordResetEmail(eq(user.getEmail()), raw.capture());
        assertThat(raw.getValue()).matches("[A-Za-z0-9_-]{43}");
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(raw.getValue().getBytes(StandardCharsets.US_ASCII)));
        assertThat(record.getValue().getTokenHash()).isEqualTo(expectedHash).isNotEqualTo(raw.getValue());
        assertThat(record.getValue().getExpiresAt()).isBetween(before.plusSeconds(1800), Instant.now().plusSeconds(1800));
        assertThat(record.getValue().getUsedAt()).isNull();
        assertThat(record.getValue().getUser()).isSameAs(user);
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        verify(users, never()).save(any());
        verifyNoInteractions(refreshTokens);
    }

    @Test
    void missingInactiveAndUnverifiedAccountsDoNotSendEmailOrChangeSessions() {
        when(users.findForPasswordResetByEmail(user.getEmail())).thenReturn(Optional.empty());
        service.forgotPassword(user.getEmail());
        when(users.findForPasswordResetByEmail(user.getEmail())).thenReturn(Optional.of(user));
        user.setIsActive(false);
        service.forgotPassword(user.getEmail());
        user.setIsActive(true);
        user.setEmailVerifiedAt(null);
        service.forgotPassword(user.getEmail());
        verifyNoInteractions(tokens, mail, refreshTokens);
        verify(users, never()).save(any());
    }

    @Test
    void mailFailurePropagatesWithoutChangingPasswordOrRevokingSessions() {
        when(users.findForPasswordResetByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doThrow(new ApiException(ErrorCode.EMAIL_SEND_FAILED)).when(mail).sendPasswordResetEmail(anyString(), anyString());
        String originalHash = user.getPasswordHash();

        assertThatThrownBy(() -> service.forgotPassword(user.getEmail()))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));

        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        verifyNoInteractions(refreshTokens);
        verify(users, never()).save(any());
    }

    @Test
    void validTokenChangesPasswordConsumesLinksAndRevokesOnlyItsUsersSessions() {
        PasswordResetToken token = validToken();
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        service.resetPassword(request);

        assertThat(passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("CurrentPass123!", user.getPasswordHash())).isFalse();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getIsActive()).isTrue();
        InOrder order = inOrder(users, tokens, refreshTokens);
        order.verify(users).findByPasswordResetTokenHash(anyString());
        order.verify(tokens).findByTokenHash(anyString());
        order.verify(users).save(user);
        order.verify(tokens).invalidateAllForUser(user.getId(), token.getUsedAt());
        order.verify(refreshTokens).revokeAllForUser(user.getId());
        verifyNoMoreInteractions(refreshTokens);
        verifyNoInteractions(mail);
    }

    @Test
    void missingTokenDoesNotChangeAnyAccount() {
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.empty());

        assertRejected(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);

        verifyNoInteractions(tokens);
    }

    @Test
    void tokenRemovedBeforeLookupIsRejected() {
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertRejected(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
    }

    @Test
    void expiredTokenCannotChangePassword() {
        PasswordResetToken token = validToken();
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertRejected(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);

        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void usedTokenCannotBeReused() {
        PasswordResetToken token = validToken();
        token.setUsedAt(Instant.now());
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertRejected(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
    }

    @Test
    void passwordResetCannotUnlockAnInactiveAccount() {
        PasswordResetToken token = validToken();
        user.setIsActive(false);
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertRejected(ErrorCode.ACCOUNT_INACTIVE);

        assertThat(user.getIsActive()).isFalse();
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void passwordResetDoesNotBypassEmailVerification() {
        PasswordResetToken token = validToken();
        user.setEmailVerifiedAt(null);
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertRejected(ErrorCode.EMAIL_NOT_VERIFIED);

        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void unchangedPasswordIsRejectedWithoutConsumingLink() {
        PasswordResetToken token = validToken();
        request.setNewPassword("CurrentPass123!");
        when(users.findByPasswordResetTokenHash(anyString())).thenReturn(Optional.of(user));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertRejected(ErrorCode.VALIDATION_ERROR);

        assertThat(token.getUsedAt()).isNull();
    }

    private PasswordResetToken validToken() {
        return PasswordResetToken.builder().user(user).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    private void assertRejected(ErrorCode code) {
        String originalHash = user.getPasswordHash();
        assertThatThrownBy(() -> service.resetPassword(request))
            .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getErrorCode()).isEqualTo(code));
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        verify(users, never()).save(any());
        verify(tokens, never()).invalidateAllForUser(anyLong(), any());
        verifyNoInteractions(refreshTokens, mail);
    }
}
