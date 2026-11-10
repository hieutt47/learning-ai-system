package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseArchiveRequest {
    @NotNull(message = "archived is required")
    private Boolean archived;
}
