package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.QuestionOption;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findByQuestionIdOrderByOrderIndexAsc(Long questionId);

    List<QuestionOption> findByQuestionIdIn(List<Long> questionIds);

    void deleteByQuestionId(Long questionId);
}
