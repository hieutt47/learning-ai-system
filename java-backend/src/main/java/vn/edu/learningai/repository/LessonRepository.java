package vn.edu.learningai.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.entity.Lesson;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    @Query("select lesson.courseId from Lesson lesson where lesson.id = :lessonId")
    Optional<Long> findCourseIdById(@Param("lessonId") Long lessonId);

    @Query("select lesson.status from Lesson lesson where lesson.id = :lessonId")
    Optional<LessonStatus> findStatusById(@Param("lessonId") Long lessonId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lesson from Lesson lesson where lesson.id = :lessonId")
    Optional<Lesson> findByIdForUpdate(@Param("lessonId") Long lessonId);

    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    List<Lesson> findByCourseIdAndStatusOrderByOrderIndexAsc(Long courseId, LessonStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lesson from Lesson lesson where lesson.courseId = :courseId order by lesson.orderIndex")
    List<Lesson> findByCourseIdForUpdate(Long courseId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatus(Long courseId, LessonStatus status);

    @Query("SELECT l.courseId AS courseId, COUNT(l) AS total FROM Lesson l "
        + "WHERE l.courseId IN :courseIds GROUP BY l.courseId")
    List<CourseCountRow> countByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Query("SELECT l.courseId AS courseId, COUNT(l) AS total FROM Lesson l "
        + "WHERE l.courseId IN :courseIds AND l.status = :status GROUP BY l.courseId")
    List<CourseCountRow> countByCourseIdsAndStatus(@Param("courseIds") List<Long> courseIds,
        @Param("status") LessonStatus status);

    @Query(value = "SELECT COUNT(*) FROM courses WHERE id = :courseId AND teacher_id = :userId", nativeQuery = true)
    long countCourseOwner(Long courseId, Long userId);

    @Query(value = "SELECT COUNT(*) FROM course_members m JOIN courses c ON c.id = m.course_id "
        + "WHERE m.course_id = :courseId AND m.student_id = :userId "
        + "AND c.is_published = TRUE AND c.archived_at IS NULL", nativeQuery = true)
    long countCourseMember(Long courseId, Long userId);

    @Query(value = "SELECT COUNT(*) FROM course_members WHERE course_id = :courseId AND student_id = :userId",
        nativeQuery = true)
    long countRawCourseMember(Long courseId, Long userId);

    @Query(value = "SELECT COUNT(*) FROM courses WHERE id = :courseId", nativeQuery = true)
    long countCourse(Long courseId);

    @Query(value = "SELECT COUNT(*) FROM quiz_attempts a JOIN quizzes q ON q.id = a.quiz_id WHERE q.lesson_id = :lessonId",
        nativeQuery = true)
    long countQuizAttempts(Long lessonId);

    @Query(value = "SELECT q.lesson_id AS lessonId, COUNT(*) AS total FROM quiz_attempts a "
        + "JOIN quizzes q ON q.id = a.quiz_id WHERE q.lesson_id IN (:lessonIds) GROUP BY q.lesson_id",
        nativeQuery = true)
    List<LessonCountRow> countQuizAttemptsByLessonIds(@Param("lessonIds") List<Long> lessonIds);

    @Query(value = "SELECT COUNT(*) FROM quizzes WHERE lesson_id = :lessonId", nativeQuery = true)
    long countQuizzes(Long lessonId);

    @Query(value = "SELECT lesson_id AS lessonId, COUNT(*) AS total FROM quizzes "
        + "WHERE lesson_id IN (:lessonIds) GROUP BY lesson_id", nativeQuery = true)
    List<LessonCountRow> countQuizzesByLessonIds(@Param("lessonIds") List<Long> lessonIds);

    @Query(value = "SELECT COUNT(*) FROM quizzes WHERE lesson_id = :lessonId AND status = 'PUBLISHED'",
        nativeQuery = true)
    long countPublishedQuizzes(Long lessonId);

    @Query(value = "SELECT lesson_id AS lessonId, COUNT(*) AS total FROM quizzes "
        + "WHERE lesson_id IN (:lessonIds) AND status = 'PUBLISHED' GROUP BY lesson_id", nativeQuery = true)
    List<LessonCountRow> countPublishedQuizzesByLessonIds(@Param("lessonIds") List<Long> lessonIds);

    @Query(value = "SELECT COUNT(*) FROM flashcards WHERE lesson_id = :lessonId", nativeQuery = true)
    long countFlashcards(Long lessonId);

    @Query(value = "SELECT lesson_id AS lessonId, COUNT(*) AS total FROM flashcards "
        + "WHERE lesson_id IN (:lessonIds) GROUP BY lesson_id", nativeQuery = true)
    List<LessonCountRow> countFlashcardsByLessonIds(@Param("lessonIds") List<Long> lessonIds);

    @Query(value = "SELECT COUNT(*) FROM flashcards WHERE lesson_id = :lessonId AND status = 'PUBLISHED'",
        nativeQuery = true)
    long countPublishedFlashcards(Long lessonId);

    @Query(value = "SELECT lesson_id AS lessonId, COUNT(*) AS total FROM flashcards "
        + "WHERE lesson_id IN (:lessonIds) AND status = 'PUBLISHED' GROUP BY lesson_id", nativeQuery = true)
    List<LessonCountRow> countPublishedFlashcardsByLessonIds(@Param("lessonIds") List<Long> lessonIds);

    @Modifying
    @Query(value = "DELETE FROM ai_generations WHERE lesson_id = :lessonId", nativeQuery = true)
    void deleteAiGenerations(@Param("lessonId") Long lessonId);
}
