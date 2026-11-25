package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.QuizStatus;

import java.math.BigDecimal;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CourseQuizStatisticsItem(
    String quizId,
    String quizTitle,
    String lessonId,
    String lessonTitle,
    int lessonOrderIndex,
    LessonStatus lessonStatus,
    QuizStatus status,
    long questionCount,
    long submittedAttempts,
    long uniqueStudents,
    BigDecimal averageScore,
    Instant latestSubmittedAt
) {}
