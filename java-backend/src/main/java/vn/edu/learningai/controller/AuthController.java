package vn.edu.learningai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.learningai.dto.request.LoginRequest;
import vn.edu.learningai.dto.request.RegisterRequest;
import vn.edu.learningai.dto.response.AuthResponse;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.security.service.RefreshTokenCookieService;
import vn.edu.learningai.service.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authenticationResponse(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
        @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
        return authenticationResponse(authService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {

        authService.logout(refreshToken);

        return ResponseEntity.noContent()
            .header(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookieService.clear().toString())
            .build();
    }

    private ResponseEntity<AuthResponse> authenticationResponse(AuthService.AuthTokenResult tokens) {
        AuthResponse response = AuthResponse.builder()
            .accessToken(tokens.getAccessToken())
            .build();

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookieService
                    .create(tokens.getRefreshToken())
                    .toString())
            .body(response);
    }

}
