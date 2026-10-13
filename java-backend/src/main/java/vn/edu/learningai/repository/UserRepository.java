package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
