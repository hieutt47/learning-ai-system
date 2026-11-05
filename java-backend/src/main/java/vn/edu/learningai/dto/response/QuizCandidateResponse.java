package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Response POST /ai/generate-quiz: wrapper {generation_id, lesson_id, source_truncated, candidate}.
 * source_truncated = true khi nguồn dài hơn giới hạn và AI chỉ nhận phần đầu (field bổ sung, chờ lead duyệt).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record QuizCandidateResponse(
    String generationId,
    String lessonId,
    Boolean sourceTruncated,
    QuizCandidate candidate
) {
}
