package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import vn.edu.learningai.common.enums.LessonStatus;

public record LessonStatusRequest(@NotNull LessonStatus status) {
}
