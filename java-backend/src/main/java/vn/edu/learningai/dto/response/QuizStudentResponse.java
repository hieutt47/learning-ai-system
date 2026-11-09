package vn.edu.learningai.dto.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import vn.edu.learningai.common.enums.QuizStatus;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record QuizStudentResponse(String id, String lessonId, String title, QuizStatus status,
                                  List<StudentQuestion> questions) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StudentQuestion(String id, String content, Integer orderIndex, List<StudentOption> options) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StudentOption(String id, String content, Integer orderIndex) {
    }
}
