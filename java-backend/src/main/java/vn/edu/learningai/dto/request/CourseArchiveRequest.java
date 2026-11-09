package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of PATCH /courses/{id}/archive: {@code {"archived": true}}.
 *
 * <p>Archiving is one-way in the MVP (contracts/payloads.md only documents
 * {@code archived=true}), so {@code false} is rejected with a validation error
 * rather than silently ignored.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseArchiveRequest {
    @NotNull(message = "archived is required")
    private Boolean archived;
}
