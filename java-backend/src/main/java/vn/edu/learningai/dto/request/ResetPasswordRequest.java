package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "Reset token must not be blank")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Invalid password reset token")
    private String token;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 10, max = 72, message = "New password must contain between 10 and 72 characters")
    private String newPassword;
}
