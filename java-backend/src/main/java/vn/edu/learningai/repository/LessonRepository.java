package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import vn.edu.learningai.entity.Lesson;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lesson from Lesson lesson where lesson.courseId = :courseId order by lesson.orderIndex")
    List<Lesson> findByCourseIdForUpdate(Long courseId);

    long countByCourseId(Long courseId);

    /** Lesson counts for a page of courses in one query. */
    @Query("SELECT l.courseId AS courseId, COUNT(l) AS total FROM Lesson l "
        + "WHERE l.courseId IN :courseIds GROUP BY l.courseId")
    List<CourseCountRow> countByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Query(value = "SELECT COUNT(*) FROM courses WHERE id = :courseId AND teacher_id = :userId", nativeQuery = true)
    long countCourseOwner(Long courseId, Long userId);

    @Query(value = "SELECT COUNT(*) FROM course_members WHERE course_id = :courseId AND student_id = :userId", nativeQuery = true)
    long countCourseMember(Long courseId, Long userId);

    @Query(value = "SELECT COUNT(*) FROM courses WHERE id = :courseId", nativeQuery = true)
    long countCourse(Long courseId);

    @Query(value = "SELECT COUNT(*) FROM quiz_attempts a JOIN quizzes q ON q.id = a.quiz_id WHERE q.lesson_id = :lessonId", nativeQuery = true)
    long countQuizAttempts(Long lessonId);

    @Query(value = "SELECT COUNT(*) FROM quizzes WHERE lesson_id = :lessonId", nativeQuery = true)
    long countQuizzes(Long lessonId);

    @Query(value = "SELECT COUNT(*) FROM flashcards WHERE lesson_id = :lessonId", nativeQuery = true)
    long countFlashcards(Long lessonId);
}
