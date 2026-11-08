package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByEmail(String email);
}
