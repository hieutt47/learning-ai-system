package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StudentBestScoreResponse(
    String studentId,
    String studentName,
    BigDecimal score,
    long attemptCount,
    Instant latestSubmittedAt
) {}
