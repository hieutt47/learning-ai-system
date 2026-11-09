package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.LessonDocument;

import java.util.List;
import java.util.Optional;

public interface LessonDocumentRepository extends JpaRepository<LessonDocument, Long> {
    List<LessonDocument> findByLessonIdOrderByCreatedAtAsc(Long lessonId);

    Optional<LessonDocument> findByIdAndLessonId(Long id, Long lessonId);

    long countByLessonId(Long lessonId);
}
