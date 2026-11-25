package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CourseStatisticsResponse(
    String courseId,
    String courseTitle,
    long enrolledStudents,
    long lessonCount,
    long publishedLessonCount,
    long quizCount,
    long submittedAttempts,
    long completedStudents,
    BigDecimal completionRate,
    BigDecimal averageScore,
    List<CourseQuizStatisticsItem> quizzes,
    Instant generatedAt
) {}
