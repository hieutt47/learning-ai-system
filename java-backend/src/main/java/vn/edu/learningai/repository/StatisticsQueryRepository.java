package vn.edu.learningai.repository;

import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.QuizStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port truy vấn đọc chuyên biệt cho module thống kê (tách SQL khỏi business service theo DIP).
 */
public interface StatisticsQueryRepository {

    Optional<CourseContext> findCourseContext(long courseId);

    CourseAggregate aggregateCourse(long courseId);

    List<CourseQuizRow> findCourseQuizzes(long courseId);

    Optional<QuizContext> findQuizContext(long quizId);

    QuizAttemptAggregate aggregateQuizAttempts(long quizId);

    List<StudentBestScoreRow> findQuizBestScores(long quizId);

    List<HardestQuestionRow> findQuizHardestQuestions(long quizId);

    SystemAggregate aggregateSystem();

    record CourseContext(long courseId, long teacherId, String title) {}

    record CourseAggregate(
        long enrolledStudents,
        long lessonCount,
        long publishedLessonCount,
        long publishedQuizCount,
        long submittedAttempts,
        long completedStudents,
        BigDecimal rawAverageScore
    ) {}

    record CourseQuizRow(
        long quizId,
        String quizTitle,
        long lessonId,
        String lessonTitle,
        int lessonOrderIndex,
        LessonStatus lessonStatus,
        QuizStatus status,
        long questionCount,
        long submittedAttempts,
        long uniqueStudents,
        BigDecimal rawAverageScore,
        Instant latestSubmittedAt
    ) {}

    record QuizContext(
        long quizId,
        String quizTitle,
        QuizStatus quizStatus,
        long courseId,
        long teacherId,
        String courseTitle,
        long lessonId,
        String lessonTitle,
        LessonStatus lessonStatus,
        long questionCount
    ) {}

    record QuizAttemptAggregate(
        long submittedAttempts,
        long uniqueStudents,
        BigDecimal rawAverageScore,
        BigDecimal highestScore,
        BigDecimal lowestScore
    ) {}

    record StudentBestScoreRow(
        long studentId,
        String studentName,
        BigDecimal bestScore,
        long attemptCount,
        Instant latestSubmittedAt
    ) {}

    record HardestQuestionRow(
        long questionId,
        int orderIndex,
        String questionContent,
        long wrongCount
    ) {}

    record SystemAggregate(
        long activeUsers,
        long teachers,
        long publishedCourses,
        long enrollments,
        long submittedAttempts
    ) {}
}
