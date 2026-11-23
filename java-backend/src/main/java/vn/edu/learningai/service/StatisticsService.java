package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.security.SecurityHelper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    private final JdbcClient jdbc;
    private final SecurityHelper security;

    public CourseStatistics course(long id) {
        requireCourseOwner(id);
        long enrolled = count("SELECT COUNT(*) FROM course_members WHERE course_id=:id", id);
        long quizzes = count("SELECT COUNT(*) FROM quizzes q JOIN lessons l ON l.id=q.lesson_id WHERE l.course_id=:id AND l.status='PUBLISHED' AND q.status='PUBLISHED'", id);
        long submitted = count("SELECT COUNT(*) FROM quiz_attempts a JOIN quizzes q ON q.id=a.quiz_id JOIN lessons l ON l.id=q.lesson_id WHERE l.course_id=:id AND a.status='SUBMITTED'", id);
        long completed = quizzes == 0 ? 0 : count("""
            SELECT COUNT(*) FROM course_members m WHERE m.course_id=:id AND NOT EXISTS (
              SELECT 1 FROM quizzes q JOIN lessons l ON l.id=q.lesson_id
              WHERE l.course_id=:id AND l.status='PUBLISHED' AND q.status='PUBLISHED' AND NOT EXISTS (
                SELECT 1 FROM quiz_attempts a WHERE a.quiz_id=q.id AND a.student_id=m.student_id
                AND a.status='SUBMITTED'))
            """, id);
        BigDecimal rate = enrolled == 0 || quizzes == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(enrolled), 2, RoundingMode.HALF_UP);
        return new CourseStatistics(Long.toString(id), enrolled, quizzes, submitted, completed, rate, Instant.now());
    }

    public QuizStatistics quiz(long id) {
        long course = jdbc.sql("SELECT l.course_id FROM quizzes q JOIN lessons l ON l.id=q.lesson_id WHERE q.id=:id")
            .param("id",id).query(Long.class).optional().orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireCourseOwner(course);
        var aggregate = jdbc.sql("""
            SELECT COUNT(*) AS submitted, COUNT(DISTINCT student_id) AS students, AVG(score) AS average_score
            FROM quiz_attempts WHERE quiz_id=:id AND status='SUBMITTED'
            """).param("id",id).query((rs,n) -> new Aggregate(rs.getLong("submitted"),rs.getLong("students"),rs.getBigDecimal("average_score"))).single();
        var best = jdbc.sql("""
            SELECT student_id,MAX(score) AS score FROM quiz_attempts
            WHERE quiz_id=:id AND status='SUBMITTED' GROUP BY student_id ORDER BY score DESC,student_id
            """).param("id",id).query((rs,n) -> new BestScore(Long.toString(rs.getLong("student_id")),rs.getBigDecimal("score"))).list();
        var hardest = jdbc.sql("""
            SELECT q.id,COUNT(*) AS wrong_count FROM questions q
            JOIN quiz_attempts a ON a.quiz_id=q.quiz_id AND a.status='SUBMITTED'
            LEFT JOIN student_answers s ON s.attempt_id=a.id AND s.question_id=q.id
            WHERE q.quiz_id=:id AND (s.id IS NULL OR s.is_correct=FALSE)
            GROUP BY q.id ORDER BY wrong_count DESC,q.id
            """).param("id",id).query((rs,n) -> new HardQuestion(Long.toString(rs.getLong("id")),rs.getLong("wrong_count"))).list();
        BigDecimal average = aggregate.average() == null ? null
            : aggregate.average().setScale(2, RoundingMode.HALF_UP);
        return new QuizStatistics(Long.toString(id),aggregate.submitted(),aggregate.students(),
            average,best,hardest,Instant.now());
    }

    public SystemStatistics system() {
        if (security.getCurrentUser().getRole()!=UserRole.ADMIN) throw new ApiException(ErrorCode.FORBIDDEN);
        return new SystemStatistics(scalar("SELECT COUNT(*) FROM users WHERE is_active=TRUE"),
            scalar("SELECT COUNT(*) FROM users WHERE role='TEACHER'"),
            scalar("SELECT COUNT(*) FROM courses WHERE is_published=TRUE AND archived_at IS NULL"),
            scalar("SELECT COUNT(*) FROM course_members"),
            scalar("SELECT COUNT(*) FROM quiz_attempts WHERE status='SUBMITTED'"),Instant.now());
    }

    private void requireCourseOwner(long id) {
        var actor=security.getCurrentUser();
        long owner=jdbc.sql("SELECT teacher_id FROM courses WHERE id=:id").param("id",id)
            .query(Long.class).optional().orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (actor.getRole()!=UserRole.ADMIN && (actor.getRole()!=UserRole.TEACHER || owner!=actor.getId().longValue())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }
    private long count(String sql,long id) { return jdbc.sql(sql).param("id",id).query(Long.class).single(); }
    private long scalar(String sql) { return jdbc.sql(sql).query(Long.class).single(); }
    private record Aggregate(long submitted,long students,BigDecimal average) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CourseStatistics(String courseId,long enrolledStudents,long quizCount,long submittedAttempts,
        long completedStudents,BigDecimal completionRate,Instant generatedAt) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record QuizStatistics(String quizId,long submittedAttempts,long uniqueStudents,BigDecimal averageScore,
        List<BestScore> bestScores,List<HardQuestion> hardestQuestions,Instant generatedAt) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BestScore(String studentId,BigDecimal score) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record HardQuestion(String questionId,long wrongCount) {}
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SystemStatistics(long activeUsers,long teachers,long publishedCourses,long enrollments,long submittedAttempts,Instant generatedAt) {}
}
