package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.entity.AuthSession;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    @Modifying
    @Query("""
            update AuthSession s
            set s.revokedAt = :now, s.updatedAt = :now
            where s.user.id = :userId and s.revokedAt is null
           """)
    public void revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);
}
