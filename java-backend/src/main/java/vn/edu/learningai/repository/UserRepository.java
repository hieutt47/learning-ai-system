package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> findForPasswordResetByEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u from User u where u.id = (
            select t.user.id from PasswordResetToken t where t.tokenHash = :tokenHash
        )
        """)
    Optional<User> findByPasswordResetTokenHash(@Param("tokenHash") String tokenHash);

    @Query("select u from User u where :email is null or lower(u.email) like :email escape '!'")
    public Page<User> searchByEmail(@Param("email") String email, Pageable pageable);
}
