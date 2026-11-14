package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.entity.EmailVerificationToken;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.EmailVerificationTokenRepository;
import vn.edu.learningai.repository.UserRepository;
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
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void sendVerificationEmail(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(EmailVerificationToken.builder()
            .user(user)
            .tokenHash(hash(token))
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .build());
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verification = tokenRepository.findByTokenHash(hash(token))
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN));
        Instant now = Instant.now();
        if (verification.getUsedAt() != null || !verification.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN);
        }
        User user = verification.getUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException(ErrorCode.ACCOUNT_INACTIVE);
        }
        verification.setUsedAt(now);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(now);
            userRepository.save(user);
        }
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || user.getEmailVerifiedAt() != null || !Boolean.TRUE.equals(user.getIsActive())) {
            return;
        }
        sendVerificationEmail(user);
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
