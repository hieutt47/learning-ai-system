package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import vn.edu.learningai.common.enums.LessonStatus;
import vn.edu.learningai.common.enums.QuizStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record QuizStatisticsResponse(
    String quizId,
    String quizTitle,
    QuizStatus quizStatus,
    String courseId,
    String courseTitle,
    String lessonId,
    String lessonTitle,
    LessonStatus lessonStatus,
    long questionCount,
    long submittedAttempts,
    long uniqueStudents,
    BigDecimal averageScore,
    BigDecimal highestScore,
    BigDecimal lowestScore,
    List<StudentBestScoreResponse> bestScores,
    List<HardestQuestionResponse> hardestQuestions,
    Instant generatedAt
) {}
