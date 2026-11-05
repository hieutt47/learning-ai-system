package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import vn.edu.learningai.common.enums.Difficulty;
import vn.edu.learningai.common.enums.SourceType;

/**
 * Body POST /ai/generate-quiz và /ai/generate-flashcards.
 * document_id bắt buộc khi source_type=DOCUMENT; difficulty bắt buộc khi sinh quiz (kiểm tra ở service).
 */
public record GenerationRequest(
    @NotNull Long lessonId,
    @NotNull SourceType sourceType,
    Long documentId,
    @NotNull @Min(1) @Max(10) Integer count,
    Difficulty difficulty
) {
}
