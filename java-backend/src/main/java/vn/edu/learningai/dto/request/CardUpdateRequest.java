package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body PATCH /flashcards/{card_id}: chỉ cập nhật field được gửi; field gửi lên không được rỗng. Chỉ khi Draft. */
public record CardUpdateRequest(
    @Size(max = 4000) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String frontText,
    @Size(max = 4000) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String backText
) {
}
