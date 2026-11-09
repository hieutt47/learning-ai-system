package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import vn.edu.learningai.common.enums.QuizStatus;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record QuizSummaryResponse(String id, String lessonId, String title, QuizStatus status, long questionCount) {
}
