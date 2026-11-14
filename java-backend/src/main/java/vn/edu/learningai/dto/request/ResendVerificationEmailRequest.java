package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResendVerificationEmailRequest {
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    @Size(max = 254)
    private String email;
}
