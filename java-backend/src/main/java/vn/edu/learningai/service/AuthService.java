package vn.edu.learningai.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.dto.request.ChangePasswordRequest;
import vn.edu.learningai.dto.request.LoginRequest;
import vn.edu.learningai.dto.request.RegisterRequest;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.UserMapper;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.SecurityHelper;
import vn.edu.learningai.security.custom.CustomUserDetails;
import vn.edu.learningai.security.jwt.JwtService;
import vn.edu.learningai.security.service.RefreshTokenService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class AuthTokenResult {
        private String accessToken;
        private String refreshToken;
    }

    public AuthTokenResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getId()).getRefreshToken();

        return AuthTokenResult.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        User user = userMapper.toUser(request);

        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setIsActive(true);

        try {
            return userMapper.toUserResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public AuthTokenResult refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new ApiException(ErrorCode.UNAUTHENTICATED);

        RefreshTokenService.RefreshTokenResult refreshTokenResul = refreshTokenService.rotateRefreshToken(refreshToken);

        User user = userRepository.findById(refreshTokenResul.getUserId())
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

        CustomUserDetails userDetails = CustomUserDetails.build(user);

        if (!userDetails.isEnabled()) throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = refreshTokenResul.getRefreshToken();

        return AuthTokenResult.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = securityHelper.getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.fieldError("current_password", "Current password is incorrect");
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw ApiException.fieldError("new_password", "New password must differ from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public void logoutAll() {
        User user = securityHelper.getCurrentUser();
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);

    }

}
