package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Flashcard do AI gợi ý, đã qua validate. JSON: {front_text, back_text}. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CardCandidate(String frontText, String backText) {
}
