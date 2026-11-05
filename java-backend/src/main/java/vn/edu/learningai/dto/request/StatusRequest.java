package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import vn.edu.learningai.common.enums.FlashcardStatus;

/** Body PATCH .../publication: status = PUBLISHED hoặc CLOSED. */
public record StatusRequest(@NotNull FlashcardStatus status) {
}
