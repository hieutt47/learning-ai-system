package vn.edu.learningai.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.entity.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuizIdOrderByOrderIndexAsc(Long quizId);

    @Query("select q.quizId from Question q where q.id = :id")
    Optional<Long> findQuizIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Question q where q.id = :id and q.quizId = :quizId")
    Optional<Question> findByIdAndQuizIdForUpdate(@Param("id") Long id, @Param("quizId") Long quizId);

    long countByQuizId(Long quizId);

    @Query("select q.quizId as quizId, count(q) as questionCount from Question q where q.quizId in :quizIds group by q.quizId")
    List<QuizQuestionCount> countByQuizIds(@Param("quizIds") List<Long> quizIds);

    interface QuizQuestionCount {
        Long getQuizId();
        Long getQuestionCount();
    }

    boolean existsByQuizIdAndOrderIndex(Long quizId, Integer orderIndex);

    boolean existsByQuizIdAndOrderIndexAndIdNot(Long quizId, Integer orderIndex, Long id);
}
