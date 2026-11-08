package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateCategoryRequest {
    @Size(max = 120, message = "Name must not exceed 120 characters")
    private String name;

    private String description;
}
