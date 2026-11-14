package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VerifyEmailRequest {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Invalid verification token")
    private String token;
}
