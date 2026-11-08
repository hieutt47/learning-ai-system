package vn.edu.learningai.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One entry of "my courses": the {@code CoursePublic} shape plus
 * {@code enrolled_at}, flattened as contracts/payloads.md specifies.
 *
 * <p>The public course is unwrapped rather than nested, so a client that can
 * render a marketplace card renders this row with the same code. There is one
 * definition of what a public course looks like: {@link CoursePublicResponse}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollmentResponse {
    @JsonUnwrapped
    private CoursePublicResponse course;

    private Instant enrolledAt;
}
