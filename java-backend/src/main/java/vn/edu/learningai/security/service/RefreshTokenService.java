package vn.edu.learningai.security.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.entity.AuthSession;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.AuthSessionRepository;
import vn.edu.learningai.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final int TOKEN_BYTES = 32;

    private final AuthSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-ttl}")
    private long refreshTTL;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class RefreshTokenResult {
        private final Long userId;
        private final Long sessionId;
        private final String refreshToken;
        private final Instant expiresAt;
    }


    public RefreshTokenResult createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        Instant expiresAt = Instant.now().plusMillis(refreshTTL);

        String token = generateToken();
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setRefreshTokenHash(hash(token));
        session.setExpiresAt(expiresAt);
        sessionRepository.save(session);

        return RefreshTokenResult.builder()
            .refreshToken(token)
            .sessionId(session.getId())
            .userId(session.getUser().getId())
            .expiresAt(expiresAt)
            .build();
    }


    public RefreshTokenResult rotateRefreshToken(String token) {
        if(token == null || token.isBlank())
            throw new ApiException(ErrorCode.UNAUTHORIZED);

        String tokenHash = hash(token);

        AuthSession session = sessionRepository.findByRefreshTokenHash(tokenHash)
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        Instant now = Instant.now();
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(now)
            || !Boolean.TRUE.equals(session.getUser().getIsActive())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        Instant expiresAt = Instant.now().plusMillis(refreshTTL);

        String replacement = generateToken();
        session.setRefreshTokenHash(hash(replacement));
        session.setExpiresAt(expiresAt);

        return RefreshTokenResult.builder()
            .refreshToken(replacement)
            .sessionId(session.getId())
            .userId(session.getUser().getId())
            .expiresAt(expiresAt)
            .build();
    }



    public void revokeRefreshToken(String token) {
        if(token == null || token.isBlank())
            return;

        Optional<AuthSession> optionalAuthSession = sessionRepository.findByRefreshTokenHash(hash(token));

        if(optionalAuthSession.isEmpty())
            return;

        if (optionalAuthSession.get().getRevokedAt() == null)
            optionalAuthSession.get().setRevokedAt(Instant.now());

    }


    public void revokeAllForUser(Long userId) {
        sessionRepository.revokeAllForUser(userId, Instant.now());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }




    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }


}
