package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.learningai.entity.EmailVerificationToken;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.EmailVerificationTokenRepository;
import vn.edu.learningai.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    @Mock EmailVerificationTokenRepository tokens;
    @Mock UserRepository users;
    @Mock(mockMaker = MockMakers.SUBCLASS) EmailService mail;
    EmailVerificationService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(tokens, users, mail);
        user = User.builder().id(5L).email("student@example.test").build();
    }

    @Test
    void issuingTokenStoresOnlyHashAndSendsTheRawToken() {
        service.sendVerificationEmail(user);
        ArgumentCaptor<EmailVerificationToken> record = ArgumentCaptor.forClass(EmailVerificationToken.class);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(tokens).save(record.capture());
        verify(mail).sendVerificationEmail(eq(user.getEmail()), token.capture());
        assertThat(token.getValue()).matches("[A-Za-z0-9_-]{43}");
        assertThat(record.getValue().getTokenHash()).matches("[a-f0-9]{64}").isNotEqualTo(token.getValue());
        assertThat(record.getValue().getExpiresAt()).isAfter(Instant.now().plusSeconds(86000));
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    void validTokenVerifiesEmailWithoutChangingAccountLock() {
        EmailVerificationToken token = EmailVerificationToken.builder().user(user).expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        service.verifyEmail("valid-token");
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(token.getUsedAt());
        assertThat(user.getIsActive()).isTrue();
        verify(users).save(user);
    }

    @Test
    void expiredTokenCannotVerifyEmail() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(EmailVerificationToken.builder()
            .user(user).expiresAt(Instant.now().minusSeconds(1)).build()));
        assertThatThrownBy(() -> service.verifyEmail("expired")).isInstanceOfSatisfying(ApiException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN));
        assertThat(user.getEmailVerifiedAt()).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void consumedTokenCannotBeReused() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(EmailVerificationToken.builder()
            .user(user).expiresAt(Instant.now().plusSeconds(60)).usedAt(Instant.now()).build()));
        assertThatThrownBy(() -> service.verifyEmail("used")).isInstanceOf(ApiException.class);
        verifyNoInteractions(users);
    }

    @Test
    void missingTokenIsRejected() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyEmail("missing")).isInstanceOf(ApiException.class);
    }

    @Test
    void verificationCannotUnlockDisabledAccount() {
        user.setIsActive(false);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(EmailVerificationToken.builder()
            .user(user).expiresAt(Instant.now().plusSeconds(60)).build()));
        assertThatThrownBy(() -> service.verifyEmail("disabled")).isInstanceOfSatisfying(ApiException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_INACTIVE));
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getIsActive()).isFalse();
    }

    @Test
    void resendForMissingVerifiedOrDisabledAccountDoesNotSendMail() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        service.resendVerificationEmail(" STUDENT@EXAMPLE.TEST ");
        user.setEmailVerifiedAt(Instant.now());
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        service.resendVerificationEmail(user.getEmail());
        user.setEmailVerifiedAt(null);
        user.setIsActive(false);
        service.resendVerificationEmail(user.getEmail());
        verifyNoInteractions(tokens, mail);
    }
}
