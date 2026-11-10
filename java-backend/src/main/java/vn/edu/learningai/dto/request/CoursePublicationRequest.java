package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoursePublicationRequest {
    @NotNull(message = "is_published is required")
    private Boolean isPublished;
}
