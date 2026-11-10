package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import vn.edu.learningai.common.enums.FlashcardStatus;

import java.time.Instant;

/** Card trong payloads.md: id, lesson_id, front_text, back_text, status, generated_by_ai, created_at. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CardResponse(
    String id,
    String lessonId,
    String frontText,
    String backText,
    FlashcardStatus status,
    Boolean generatedByAi,
    Instant createdAt
) {
}
