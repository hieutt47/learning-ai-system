package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import vn.edu.learningai.common.enums.QuizStatus;

import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record QuizAuthorResponse(String id, String lessonId, String title, QuizStatus status,
                                 Instant createdAt, List<QuestionAuthor> questions) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record QuestionAuthor(String id, String content, String explanation, Integer orderIndex,
                                 Boolean generatedByAi, List<OptionAuthor> options) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record OptionAuthor(String id, String content, Boolean isCorrect, Integer orderIndex) {
    }
}
