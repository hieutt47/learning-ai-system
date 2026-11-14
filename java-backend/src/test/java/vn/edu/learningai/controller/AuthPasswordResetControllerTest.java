package vn.edu.learningai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.learningai.config.RequestIdFilter;
import vn.edu.learningai.config.SecurityConfig;
import vn.edu.learningai.dto.request.ResetPasswordRequest;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.exception.GlobalExceptionHandler;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.custom.CustomAccessDeniedHandler;
import vn.edu.learningai.security.custom.CustomAuthenticationEntryPoint;
import vn.edu.learningai.security.custom.CustomUserDetailsService;
import vn.edu.learningai.security.jwt.JwtAuthenticationFilter;
import vn.edu.learningai.security.jwt.JwtService;
import vn.edu.learningai.security.service.RefreshTokenCookieService;
import vn.edu.learningai.service.AuthService;
import vn.edu.learningai.service.EmailVerificationService;
import vn.edu.learningai.service.PasswordResetService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(properties = {"spring.jackson.property-naming-strategy=SNAKE_CASE",
    "jwt.jwt-signing-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@ContextConfiguration(classes = {AuthController.class, SecurityConfig.class, JwtAuthenticationFilter.class,
    CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, GlobalExceptionHandler.class, RequestIdFilter.class,
    AuthPasswordResetControllerTest.MockConfig.class})
class AuthPasswordResetControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private AuthService authService;
    @Autowired private EmailVerificationService emailVerificationService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private RefreshTokenCookieService cookieService;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private JwtService jwtService;

    @BeforeEach
    void setUp() {
        reset(authService, emailVerificationService, passwordResetService, cookieService, userDetailsService, jwtService);
    }

    @Test
    void guestCanRequestResetWithoutReceivingTokenOrCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@example.test\"}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""))
            .andExpect(header().doesNotExist("Set-Cookie"));

        verify(passwordResetService).forgotPassword("student@example.test");
        verifyNoInteractions(authService, emailVerificationService, cookieService, jwtService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{", "{\"email\":null}", "{\"email\":\"\"}",
        "{\"email\":\" \"}", "{\"email\":\"invalid-email\"}"})
    void invalidForgotPasswordBodyReturns400(String body) throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(passwordResetService);
    }

    @Test
    void guestCanResetWithSnakeCaseBodyAndExpiredCookieIsReturned() throws Exception {
        when(cookieService.clear()).thenReturn(ResponseCookie.from("refreshToken", "")
            .path("/api/v1/auth").httpOnly(true).sameSite("Lax").maxAge(0).build());

        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(resetBody("A".repeat(43), "NewPassword123!")))
            .andExpect(status().isNoContent()).andExpect(content().string(""))
            .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));

        ArgumentCaptor<ResetPasswordRequest> request = ArgumentCaptor.forClass(ResetPasswordRequest.class);
        verify(passwordResetService).resetPassword(request.capture());
        assertThat(request.getValue().getToken()).isEqualTo("A".repeat(43));
        assertThat(request.getValue().getNewPassword()).isEqualTo("NewPassword123!");
        verifyNoInteractions(authService, emailVerificationService, jwtService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{", "{\"token\":null,\"new_password\":\"NewPassword123!\"}",
        "{\"token\":\"short\",\"new_password\":\"NewPassword123!\"}"})
    void invalidResetBodyReturns400WithoutCallingService(String body) throws Exception {
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(passwordResetService, cookieService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "         ", "123456789", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void invalidNewPasswordUsesSnakeCaseFieldInError(String password) throws Exception {
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(resetBody("A".repeat(43), password)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0].field").value("new_password"));
        verifyNoInteractions(passwordResetService, cookieService);
    }

    @Test
    void expiredOrUsedLinkReturns400WithoutClearingCookie() throws Exception {
        doThrow(new ApiException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN))
            .when(passwordResetService).resetPassword(any());
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(resetBody("A".repeat(43), "NewPassword123!")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"))
            .andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(cookieService);
    }

    @Test
    void mailFailureReturns503WithoutSettingCookie() throws Exception {
        doThrow(new ApiException(ErrorCode.EMAIL_SEND_FAILED)).when(passwordResetService).forgotPassword(anyString());
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"student@example.test\"}"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("EMAIL_SEND_FAILED"))
            .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void existingChangePasswordStillRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"current_password\":\"CurrentPass123!\",\"new_password\":\"NewPassword123!\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(authService, passwordResetService);
    }

    private String resetBody(String token, String password) {
        return "{\"token\":\"" + token + "\",\"new_password\":\"" + password + "\"}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfig {
        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        AuthService authService() {
            return mock(AuthService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }

        @Bean
        EmailVerificationService emailVerificationService() {
            return mock(EmailVerificationService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }

        @Bean
        PasswordResetService passwordResetService() {
            return mock(PasswordResetService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }

        @Bean
        RefreshTokenCookieService cookieService() {
            return mock(RefreshTokenCookieService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }

        @Bean
        CustomUserDetailsService userDetailsService() {
            return mock(CustomUserDetailsService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        }
    }
}
