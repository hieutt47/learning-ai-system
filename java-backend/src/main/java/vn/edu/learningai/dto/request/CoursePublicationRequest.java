package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of PATCH /courses/{id}/publication: {@code {"is_published": true|false}}.
 *
 * <p>Boxed and {@code @NotNull} so an omitted field is a validation error rather
 * than a silent unpublish.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoursePublicationRequest {
    @NotNull(message = "is_published is required")
    private Boolean isPublished;
}
