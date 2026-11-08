package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateTeacherRequest {
    @NotBlank(message = "Reason must not be blank")
    @Size(max = 4000, message = "Reason must not exceed 4000 characters")
    private String reason;
}
