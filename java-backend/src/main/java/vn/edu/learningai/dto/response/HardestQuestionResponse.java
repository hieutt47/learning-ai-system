package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HardestQuestionResponse(
    String questionId,
    int orderIndex,
    String questionContent,
    long wrongCount,
    BigDecimal wrongRate
) {}
