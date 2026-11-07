package vn.edu.learningai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LessonReorderRequest(
    @NotEmpty @JsonProperty("lesson_ids") List<@NotNull Long> lessonIds
) {
}
