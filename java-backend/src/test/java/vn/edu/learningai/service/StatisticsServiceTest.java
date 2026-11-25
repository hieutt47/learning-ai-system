package vn.edu.learningai.service;

import org.junit.jupiter.api.Test;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.common.enums.UserRole;
import vn.edu.learningai.entity.User;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.repository.StatisticsQueryRepository;
import vn.edu.learningai.security.SecurityHelper;
import vn.edu.learningai.service.statistics.StatisticsAccessPolicy;
import vn.edu.learningai.service.statistics.StatisticsMetricsCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatisticsServiceTest {
    private final StatisticsAccessPolicy accessPolicy = new StatisticsAccessPolicy();
    private final StatisticsMetricsCalculator calculator = new StatisticsMetricsCalculator();

    @Test
    void nonAdminCannotQuerySystemAggregates() {
        var repository = new StubStatisticsQueryRepository();
        var service = serviceFor(user(4L, UserRole.STUDENT), repository);

        assertThatThrownBy(service::system)
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(repository.systemQueried).isFalse();
    }

    @Test
    void nonOwnerTeacherAndStudentCannotViewCourseOrQuizStatistics() {
        var repository = new StubStatisticsQueryRepository();

        for (User actor : List.of(user(3L, UserRole.TEACHER), user(4L, UserRole.STUDENT))) {
            var service = serviceFor(actor, repository);
            assertThatThrownBy(() -> service.course(2L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
            assertThatThrownBy(() -> service.quiz(30L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Test
    void missingCourseOrQuizThrowsNotFound() {
        var repository = new StubStatisticsQueryRepository();
        var service = serviceFor(user(2L, UserRole.TEACHER), repository);

        assertThatThrownBy(() -> service.course(999L))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> service.quiz(999L))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void courseOwnerReceivesAggregatedCourseAndQuizBreakdown() {
        var repository = new StubStatisticsQueryRepository();
        var service = serviceFor(user(2L, UserRole.TEACHER), repository);

        var response = service.course(2L);

        assertThat(response.courseId()).isEqualTo("2");
        assertThat(response.courseTitle()).isEqualTo("Java căn bản");
        assertThat(response.enrolledStudents()).isEqualTo(3);
        assertThat(response.lessonCount()).isEqualTo(4);
        assertThat(response.publishedLessonCount()).isEqualTo(3);
        assertThat(response.quizCount()).isEqualTo(2);
        assertThat(response.submittedAttempts()).isEqualTo(3);
        assertThat(response.completedStudents()).isEqualTo(1);
        assertThat(response.completionRate()).isEqualByComparingTo("33.33");
        assertThat(response.averageScore()).isEqualByComparingTo("66.67");
        assertThat(response.quizzes()).hasSize(1);
        assertThat(response.quizzes().get(0).quizId()).isEqualTo("30");
        assertThat(response.quizzes().get(0).averageScore()).isEqualByComparingTo("75.00");
        assertThat(response.generatedAt()).isNotNull();
    }

    @Test
    void quizStatisticsIncludesStudentNamesQuestionContentAndWrongRates() {
        var repository = new StubStatisticsQueryRepository();
        var service = serviceFor(user(1L, UserRole.ADMIN), repository);

        var response = service.quiz(30L);

        assertThat(response.quizId()).isEqualTo("30");
        assertThat(response.quizTitle()).isEqualTo("Quiz JVM");
        assertThat(response.courseId()).isEqualTo("2");
        assertThat(response.courseTitle()).isEqualTo("Java căn bản");
        assertThat(response.submittedAttempts()).isEqualTo(3);
        assertThat(response.uniqueStudents()).isEqualTo(2);
        assertThat(response.averageScore()).isEqualByComparingTo("33.33");
        assertThat(response.highestScore()).isEqualByComparingTo("100.00");
        assertThat(response.lowestScore()).isEqualByComparingTo("0.00");
        assertThat(response.bestScores()).hasSize(1);
        assertThat(response.bestScores().get(0).studentName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.bestScores().get(0).attemptCount()).isEqualTo(2);
        assertThat(response.hardestQuestions()).hasSize(1);
        assertThat(response.hardestQuestions().get(0).questionContent()).isEqualTo("JVM là gì?");
        assertThat(response.hardestQuestions().get(0).wrongRate()).isEqualByComparingTo("66.67");
    }

    private StatisticsService serviceFor(User actor, StatisticsQueryRepository repository) {
        return new StatisticsService(repository, accessPolicy, calculator, new StubSecurityHelper(actor));
    }

    private static User user(Long id, UserRole role) {
        return User.builder().id(id).role(role).isActive(true).build();
    }

    private static final class StubSecurityHelper extends SecurityHelper {
        private final User actor;

        StubSecurityHelper(User actor) {
            super(null);
            this.actor = actor;
        }

        @Override
        public User getCurrentUser() {
            return actor;
        }
    }

    private static final class StubStatisticsQueryRepository implements StatisticsQueryRepository {
        private static final Instant SUBMITTED_AT = Instant.parse("2026-11-25T10:00:00Z");
        boolean systemQueried;

        @Override
        public Optional<CourseContext> findCourseContext(long courseId) {
            return courseId == 2L
                ? Optional.of(new CourseContext(2L, 2L, "Java căn bản"))
                : Optional.empty();
        }

        @Override
        public CourseAggregate aggregateCourse(long courseId) {
            return new CourseAggregate(3, 4, 3, 2, 3, 1, new BigDecimal("66.6667"));
        }

        @Override
        public List<CourseQuizRow> findCourseQuizzes(long courseId) {
            return List.of(new CourseQuizRow(
                30L, "Quiz JVM", 10L, "Bài 1: JVM", 1, LessonStatus.PUBLISHED,
                QuizStatus.PUBLISHED, 5, 2, 1, new BigDecimal("75"), SUBMITTED_AT
            ));
        }

        @Override
        public Optional<QuizContext> findQuizContext(long quizId) {
            return quizId == 30L
                ? Optional.of(new QuizContext(
                    30L, "Quiz JVM", QuizStatus.PUBLISHED, 2L, 2L, "Java căn bản",
                    10L, "Bài 1: JVM", LessonStatus.PUBLISHED, 5
                ))
                : Optional.empty();
        }

        @Override
        public QuizAttemptAggregate aggregateQuizAttempts(long quizId) {
            return new QuizAttemptAggregate(
                3, 2, new BigDecimal("33.3333"), new BigDecimal("100"), BigDecimal.ZERO
            );
        }

        @Override
        public List<StudentBestScoreRow> findQuizBestScores(long quizId) {
            return List.of(new StudentBestScoreRow(4L, "Nguyễn Văn A", new BigDecimal("100"), 2, SUBMITTED_AT));
        }

        @Override
        public List<HardestQuestionRow> findQuizHardestQuestions(long quizId) {
            return List.of(new HardestQuestionRow(101L, 0, "JVM là gì?", 2));
        }

        @Override
        public SystemAggregate aggregateSystem() {
            systemQueried = true;
            return new SystemAggregate(10, 2, 3, 20, 50);
        }
    }
}
