package vn.edu.learningai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.learningai.dto.request.ChangePasswordRequest;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.UserMapper;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.SecurityHelper;
import vn.edu.learningai.security.jwt.JwtService;
import vn.edu.learningai.security.service.RefreshTokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock(mockMaker = MockMakers.SUBCLASS) private JwtService jwtService;
    @Mock(mockMaker = MockMakers.SUBCLASS) private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;
    @Mock private UserRepository userRepository;
    @Mock(mockMaker = MockMakers.SUBCLASS) private SecurityHelper securityHelper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AuthService(authenticationManager, jwtService, refreshTokenService,
            userMapper, passwordEncoder, userRepository, securityHelper);
        user = User.builder().id(7L).passwordHash(passwordEncoder.encode("CurrentPass123!")).build();
    }

    @Test
    void passwordChangeHashesNewPasswordAndRevokesEveryRefreshSession() {
        when(securityHelper.getCurrentUser()).thenReturn(user);

        service.changePassword(new ChangePasswordRequest("CurrentPass123!", "NewPassword123!"));

        assertThat(passwordEncoder.matches("NewPassword123!", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("CurrentPass123!", user.getPasswordHash())).isFalse();
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(7L);
    }

    @Test
    void incorrectCurrentPasswordDoesNotChangePasswordOrRevokeSessions() {
        when(securityHelper.getCurrentUser()).thenReturn(user);
        String originalHash = user.getPasswordHash();

        assertThatThrownBy(() -> service.changePassword(
            new ChangePasswordRequest("WrongPassword123!", "NewPassword123!")))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                assertThat(error.getDetails().get(0).getField()).isEqualTo("current_password");
            });

        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        verifyNoInteractions(userRepository, refreshTokenService);
    }

    @Test
    void unchangedPasswordIsRejectedWithoutRevokingSessions() {
        when(securityHelper.getCurrentUser()).thenReturn(user);

        assertThatThrownBy(() -> service.changePassword(
            new ChangePasswordRequest("CurrentPass123!", "CurrentPass123!")))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getDetails().get(0).getField()).isEqualTo("new_password"));

        verifyNoInteractions(userRepository, refreshTokenService);
    }

    @Test
    void logoutAllUsesTheAuthenticatedUser() {
        when(securityHelper.getCurrentUser()).thenReturn(user);

        service.logoutAll();

        verify(refreshTokenService).revokeAllForUser(7L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void unauthenticatedCallerCannotRevokeSessions() {
        when(securityHelper.getCurrentUser()).thenThrow(new ApiException(ErrorCode.UNAUTHENTICATED));

        assertThatThrownBy(() -> service.logoutAll()).isInstanceOf(ApiException.class);

        verifyNoInteractions(refreshTokenService);
    }
}
