package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {
    @NotBlank(message = "Current password must not be blank")
    @Size(max = 72, message = "Current password must not exceed 72 characters")
    private String currentPassword;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 10, max = 72, message = "New password must contain between 10 and 72 characters")
    private String newPassword;
}
