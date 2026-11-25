package vn.edu.learningai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.QuizStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcStatisticsQueryRepository implements StatisticsQueryRepository {
    private final JdbcClient jdbc;

    @Override
    public Optional<CourseContext> findCourseContext(long courseId) {
        return jdbc.sql("SELECT id, teacher_id, title FROM courses WHERE id = :courseId")
            .param("courseId", courseId)
            .query((rs, rowNum) -> new CourseContext(
                rs.getLong("id"),
                rs.getLong("teacher_id"),
                rs.getString("title")
            ))
            .optional();
    }

    @Override
    public CourseAggregate aggregateCourse(long courseId) {
        long enrolled = countByCourse("SELECT COUNT(*) FROM course_members WHERE course_id = :courseId", courseId);
        long lessonCount = countByCourse("SELECT COUNT(*) FROM lessons WHERE course_id = :courseId", courseId);
        long publishedLessonCount = countByCourse(
            "SELECT COUNT(*) FROM lessons WHERE course_id = :courseId AND status = 'PUBLISHED'", courseId);
        long publishedQuizCount = countByCourse("""
            SELECT COUNT(*) FROM quizzes q
            JOIN lessons l ON l.id = q.lesson_id
            WHERE l.course_id = :courseId AND l.status = 'PUBLISHED' AND q.status = 'PUBLISHED'
            """, courseId);

        var attempts = jdbc.sql("""
            SELECT COUNT(*) AS submitted_attempts, AVG(a.score) AS average_score
            FROM quiz_attempts a
            JOIN quizzes q ON q.id = a.quiz_id
            JOIN lessons l ON l.id = q.lesson_id
            WHERE l.course_id = :courseId AND a.status = 'SUBMITTED'
            """)
            .param("courseId", courseId)
            .query((rs, rowNum) -> new AttemptSummary(
                rs.getLong("submitted_attempts"),
                rs.getBigDecimal("average_score")
            ))
            .single();

        long completedStudents = publishedQuizCount == 0 ? 0 : countByCourse("""
            SELECT COUNT(*) FROM course_members m
            WHERE m.course_id = :courseId AND NOT EXISTS (
                SELECT 1 FROM quizzes q
                JOIN lessons l ON l.id = q.lesson_id
                WHERE l.course_id = :courseId AND l.status = 'PUBLISHED' AND q.status = 'PUBLISHED'
                  AND NOT EXISTS (
                      SELECT 1 FROM quiz_attempts a
                      WHERE a.quiz_id = q.id AND a.student_id = m.student_id AND a.status = 'SUBMITTED'
                  )
            )
            """, courseId);

        return new CourseAggregate(
            enrolled,
            lessonCount,
            publishedLessonCount,
            publishedQuizCount,
            attempts.submittedAttempts(),
            completedStudents,
            attempts.averageScore()
        );
    }

    @Override
    public List<CourseQuizRow> findCourseQuizzes(long courseId) {
        return jdbc.sql("""
            SELECT q.id AS quiz_id,
                   q.title AS quiz_title,
                   l.id AS lesson_id,
                   l.title AS lesson_title,
                   l.order_index AS lesson_order_index,
                   l.status AS lesson_status,
                   q.status AS quiz_status,
                   (SELECT COUNT(*) FROM questions qs WHERE qs.quiz_id = q.id) AS question_count,
                   COUNT(a.id) AS submitted_attempts,
                   COUNT(DISTINCT a.student_id) AS unique_students,
                   AVG(a.score) AS average_score,
                   MAX(a.submitted_at) AS latest_submitted_at
            FROM quizzes q
            JOIN lessons l ON l.id = q.lesson_id
            LEFT JOIN quiz_attempts a ON a.quiz_id = q.id AND a.status = 'SUBMITTED'
            WHERE l.course_id = :courseId AND q.status <> 'DRAFT'
            GROUP BY q.id, q.title, l.id, l.title, l.order_index, l.status, q.status, q.created_at
            ORDER BY l.order_index ASC, q.created_at DESC, q.id DESC
            """)
            .param("courseId", courseId)
            .query((rs, rowNum) -> new CourseQuizRow(
                rs.getLong("quiz_id"),
                rs.getString("quiz_title"),
                rs.getLong("lesson_id"),
                rs.getString("lesson_title"),
                rs.getInt("lesson_order_index"),
                LessonStatus.valueOf(rs.getString("lesson_status")),
                QuizStatus.valueOf(rs.getString("quiz_status")),
                rs.getLong("question_count"),
                rs.getLong("submitted_attempts"),
                rs.getLong("unique_students"),
                rs.getBigDecimal("average_score"),
                toInstant(rs.getTimestamp("latest_submitted_at"))
            ))
            .list();
    }

    @Override
    public Optional<QuizContext> findQuizContext(long quizId) {
        return jdbc.sql("""
            SELECT q.id AS quiz_id,
                   q.title AS quiz_title,
                   q.status AS quiz_status,
                   c.id AS course_id,
                   c.teacher_id AS teacher_id,
                   c.title AS course_title,
                   l.id AS lesson_id,
                   l.title AS lesson_title,
                   l.status AS lesson_status,
                   (SELECT COUNT(*) FROM questions qs WHERE qs.quiz_id = q.id) AS question_count
            FROM quizzes q
            JOIN lessons l ON l.id = q.lesson_id
            JOIN courses c ON c.id = l.course_id
            WHERE q.id = :quizId
            """)
            .param("quizId", quizId)
            .query((rs, rowNum) -> new QuizContext(
                rs.getLong("quiz_id"),
                rs.getString("quiz_title"),
                QuizStatus.valueOf(rs.getString("quiz_status")),
                rs.getLong("course_id"),
                rs.getLong("teacher_id"),
                rs.getString("course_title"),
                rs.getLong("lesson_id"),
                rs.getString("lesson_title"),
                LessonStatus.valueOf(rs.getString("lesson_status")),
                rs.getLong("question_count")
            ))
            .optional();
    }

    @Override
    public QuizAttemptAggregate aggregateQuizAttempts(long quizId) {
        return jdbc.sql("""
            SELECT COUNT(*) AS submitted_attempts,
                   COUNT(DISTINCT student_id) AS unique_students,
                   AVG(score) AS average_score,
                   MAX(score) AS highest_score,
                   MIN(score) AS lowest_score
            FROM quiz_attempts
            WHERE quiz_id = :quizId AND status = 'SUBMITTED'
            """)
            .param("quizId", quizId)
            .query((rs, rowNum) -> new QuizAttemptAggregate(
                rs.getLong("submitted_attempts"),
                rs.getLong("unique_students"),
                rs.getBigDecimal("average_score"),
                rs.getBigDecimal("highest_score"),
                rs.getBigDecimal("lowest_score")
            ))
            .single();
    }

    @Override
    public List<StudentBestScoreRow> findQuizBestScores(long quizId) {
        return jdbc.sql("""
            SELECT a.student_id,
                   u.full_name AS student_name,
                   MAX(a.score) AS best_score,
                   COUNT(*) AS attempt_count,
                   MAX(a.submitted_at) AS latest_submitted_at
            FROM quiz_attempts a
            JOIN users u ON u.id = a.student_id
            WHERE a.quiz_id = :quizId AND a.status = 'SUBMITTED'
            GROUP BY a.student_id, u.full_name
            ORDER BY best_score DESC, a.student_id ASC
            """)
            .param("quizId", quizId)
            .query((rs, rowNum) -> new StudentBestScoreRow(
                rs.getLong("student_id"),
                rs.getString("student_name"),
                rs.getBigDecimal("best_score"),
                rs.getLong("attempt_count"),
                toInstant(rs.getTimestamp("latest_submitted_at"))
            ))
            .list();
    }

    @Override
    public List<HardestQuestionRow> findQuizHardestQuestions(long quizId) {
        return jdbc.sql("""
            SELECT q.id AS question_id,
                   q.order_index,
                   q.content AS question_content,
                   COUNT(*) AS wrong_count
            FROM questions q
            JOIN quiz_attempts a ON a.quiz_id = q.quiz_id AND a.status = 'SUBMITTED'
            LEFT JOIN student_answers s ON s.attempt_id = a.id AND s.question_id = q.id
            WHERE q.quiz_id = :quizId AND (s.id IS NULL OR s.is_correct = FALSE)
            GROUP BY q.id, q.order_index, q.content
            ORDER BY wrong_count DESC, q.id ASC
            """)
            .param("quizId", quizId)
            .query((rs, rowNum) -> new HardestQuestionRow(
                rs.getLong("question_id"),
                rs.getInt("order_index"),
                rs.getString("question_content"),
                rs.getLong("wrong_count")
            ))
            .list();
    }

    @Override
    public SystemAggregate aggregateSystem() {
        return new SystemAggregate(
            scalar("SELECT COUNT(*) FROM users WHERE is_active = TRUE"),
            scalar("SELECT COUNT(*) FROM users WHERE role = 'TEACHER'"),
            scalar("SELECT COUNT(*) FROM courses WHERE is_published = TRUE AND archived_at IS NULL"),
            scalar("SELECT COUNT(*) FROM course_members"),
            scalar("SELECT COUNT(*) FROM quiz_attempts WHERE status = 'SUBMITTED'")
        );
    }

    private long countByCourse(String sql, long courseId) {
        return jdbc.sql(sql).param("courseId", courseId).query(Long.class).single();
    }

    private long scalar(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record AttemptSummary(long submittedAttempts, BigDecimal averageScore) {}
}
