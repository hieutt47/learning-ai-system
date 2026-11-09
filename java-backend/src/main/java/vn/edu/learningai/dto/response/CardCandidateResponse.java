package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Response POST /ai/generate-flashcards: wrapper {generation_id, lesson_id, source_truncated, candidate}. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CardCandidateResponse(
    String generationId,
    String lessonId,
    Boolean sourceTruncated,
    FlashcardCandidate candidate
) {
}
