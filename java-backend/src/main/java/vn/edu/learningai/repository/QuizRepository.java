package vn.edu.learningai.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.entity.Quiz;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Page<Quiz> findByLessonId(Long lessonId, Pageable pageable);

    Page<Quiz> findByLessonIdAndStatus(Long lessonId, QuizStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findByIdForUpdate(@Param("id") Long id);
}
