package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Phương án của câu hỏi gợi ý (chưa có ID DB). JSON: {content, is_correct}. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OptionCandidate(String content, Boolean isCorrect) {
}
