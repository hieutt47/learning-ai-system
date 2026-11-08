package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateUserStatusRequest {
    @NotNull(message = "Active status must not be null")
    private Boolean isActive;
}
