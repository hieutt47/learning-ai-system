package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SystemStatisticsResponse(
    long activeUsers,
    long teachers,
    long publishedCourses,
    long enrollments,
    long submittedAttempts,
    Instant generatedAt
) {}
