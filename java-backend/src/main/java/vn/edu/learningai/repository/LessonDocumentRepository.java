package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.entity.LessonDocument;

import java.util.List;
import java.util.Optional;

public interface LessonDocumentRepository extends JpaRepository<LessonDocument, Long> {
    List<LessonDocument> findByLessonIdOrderByCreatedAtAsc(Long lessonId);

    Optional<LessonDocument> findByIdAndLessonId(Long id, Long lessonId);

    long countByLessonId(Long lessonId);

    @Query("SELECT d.lessonId AS lessonId, COUNT(d) AS total FROM LessonDocument d "
        + "WHERE d.lessonId IN :lessonIds GROUP BY d.lessonId")
    List<LessonCountRow> countByLessonIds(@Param("lessonIds") List<Long> lessonIds);
}
