package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findForPasswordResetByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive()) || user.getEmailVerifiedAt() == null) {
            return;
        }

        Instant now = Instant.now();
        tokenRepository.invalidateAllForUser(user.getId(), now);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(PasswordResetToken.builder()
            .user(user)
            .tokenHash(hash(token))
            .expiresAt(now.plus(30, ChronoUnit.MINUTES))
            .build());
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hash(request.getToken());
        User user = userRepository.findByPasswordResetTokenHash(tokenHash)
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }
        if (user.getEmailVerifiedAt() == null) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw ApiException.fieldError("new_password", "New password must differ from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        token.setUsedAt(now);
        tokenRepository.invalidateAllForUser(user.getId(), now);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
