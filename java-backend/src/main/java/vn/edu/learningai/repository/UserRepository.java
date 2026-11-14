package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByEmail(String email);

    @Query("select u from User u where :email is null or lower(u.email) like :email escape '!'")
    public Page<User> searchByEmail(@Param("email") String email, Pageable pageable);
}
