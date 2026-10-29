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
import vn.edu.learningai.dto.request.LoginRequest;
import vn.edu.learningai.dto.request.RegisterRequest;
import vn.edu.learningai.dto.response.AuthResponse;
import vn.edu.learningai.dto.response.UserResponse;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.UserMapper;
import vn.edu.learningai.repository.UserRepository;
import vn.edu.learningai.security.custom.CustomUserDetails;
import vn.edu.learningai.security.jwt.JwtService;
import vn.edu.learningai.security.service.RefreshTokenService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class AuthTokenResult {
        private String accessToken;
        private String refreshToken;
    }

    public AuthTokenResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

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

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setIsActive(true);


        try {
            return userMapper.toUserResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.USER_EXISTED);
        }
    }

    @Transactional
    public AuthTokenResult refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new ApiException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);

        RefreshTokenService.RefreshTokenResult refreshTokenResul = refreshTokenService.rotateRefreshToken(refreshToken);

        User user = userRepository.findById(refreshTokenResul.getUserId())
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        CustomUserDetails userDetails = CustomUserDetails.build(user);

        if (!userDetails.isEnabled()) throw new ApiException(ErrorCode.USER_INACTIVE);

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = refreshTokenResul.getRefreshToken();

        return AuthTokenResult.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);

    }

}
