package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuizIdOrderByOrderIndexAsc(Long quizId);

    long countByQuizId(Long quizId);

    boolean existsByQuizIdAndOrderIndex(Long quizId, Integer orderIndex);

    boolean existsByQuizIdAndOrderIndexAndIdNot(Long quizId, Integer orderIndex, Long id);
}
