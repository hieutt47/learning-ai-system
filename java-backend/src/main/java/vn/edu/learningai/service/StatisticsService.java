package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.dto.response.CourseQuizStatisticsItem;
import vn.edu.learningai.dto.response.CourseStatisticsResponse;
import vn.edu.learningai.dto.response.HardestQuestionResponse;
import vn.edu.learningai.dto.response.QuizStatisticsResponse;
import vn.edu.learningai.dto.response.StudentBestScoreResponse;
import vn.edu.learningai.dto.response.SystemStatisticsResponse;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.StatisticsQueryRepository;
import vn.edu.learningai.security.SecurityHelper;
import vn.edu.learningai.service.statistics.StatisticsAccessPolicy;
import vn.edu.learningai.service.statistics.StatisticsMetricsCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    private final StatisticsQueryRepository queryRepository;
    private final StatisticsAccessPolicy accessPolicy;
    private final StatisticsMetricsCalculator metricsCalculator;
    private final SecurityHelper security;

    public CourseStatisticsResponse course(long courseId) {
        var context = queryRepository.findCourseContext(courseId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
        accessPolicy.assertCanViewCourseStatistics(security.getCurrentUser(), context.teacherId());

        var aggregate = queryRepository.aggregateCourse(courseId);
        BigDecimal completionRate = metricsCalculator.completionRate(
            aggregate.completedStudents(),
            aggregate.enrolledStudents(),
            aggregate.publishedQuizCount()
        );
        BigDecimal averageScore = metricsCalculator.roundScore(aggregate.rawAverageScore());

        List<CourseQuizStatisticsItem> quizzes = queryRepository.findCourseQuizzes(courseId).stream()
            .map(row -> new CourseQuizStatisticsItem(
                Long.toString(row.quizId()),
                row.quizTitle(),
                Long.toString(row.lessonId()),
                row.lessonTitle(),
                row.lessonOrderIndex(),
                row.lessonStatus(),
                row.status(),
                row.questionCount(),
                row.submittedAttempts(),
                row.uniqueStudents(),
                metricsCalculator.roundScore(row.rawAverageScore()),
                row.latestSubmittedAt()
            ))
            .toList();

        return new CourseStatisticsResponse(
            Long.toString(context.courseId()),
            context.title(),
            aggregate.enrolledStudents(),
            aggregate.lessonCount(),
            aggregate.publishedLessonCount(),
            aggregate.publishedQuizCount(),
            aggregate.submittedAttempts(),
            aggregate.completedStudents(),
            completionRate,
            averageScore,
            quizzes,
            Instant.now()
        );
    }

    public QuizStatisticsResponse quiz(long quizId) {
        var context = queryRepository.findQuizContext(quizId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Quiz not found"));
        accessPolicy.assertCanViewCourseStatistics(security.getCurrentUser(), context.teacherId());

        var aggregate = queryRepository.aggregateQuizAttempts(quizId);
        List<StudentBestScoreResponse> bestScores = queryRepository.findQuizBestScores(quizId).stream()
            .map(row -> new StudentBestScoreResponse(
                Long.toString(row.studentId()),
                row.studentName(),
                metricsCalculator.roundScore(row.bestScore()),
                row.attemptCount(),
                row.latestSubmittedAt()
            ))
            .toList();

        List<HardestQuestionResponse> hardestQuestions = queryRepository.findQuizHardestQuestions(quizId).stream()
            .map(row -> new HardestQuestionResponse(
                Long.toString(row.questionId()),
                row.orderIndex(),
                row.questionContent(),
                row.wrongCount(),
                metricsCalculator.wrongRate(row.wrongCount(), aggregate.submittedAttempts())
            ))
            .toList();

        return new QuizStatisticsResponse(
            Long.toString(context.quizId()),
            context.quizTitle(),
            context.quizStatus(),
            Long.toString(context.courseId()),
            context.courseTitle(),
            Long.toString(context.lessonId()),
            context.lessonTitle(),
            context.lessonStatus(),
            context.questionCount(),
            aggregate.submittedAttempts(),
            aggregate.uniqueStudents(),
            metricsCalculator.roundScore(aggregate.rawAverageScore()),
            metricsCalculator.roundScore(aggregate.highestScore()),
            metricsCalculator.roundScore(aggregate.lowestScore()),
            bestScores,
            hardestQuestions,
            Instant.now()
        );
    }

    public SystemStatisticsResponse system() {
        accessPolicy.assertCanViewSystemStatistics(security.getCurrentUser());
        var aggregate = queryRepository.aggregateSystem();
        return new SystemStatisticsResponse(
            aggregate.activeUsers(),
            aggregate.teachers(),
            aggregate.publishedCourses(),
            aggregate.enrollments(),
            aggregate.submittedAttempts(),
            Instant.now()
        );
    }
}
