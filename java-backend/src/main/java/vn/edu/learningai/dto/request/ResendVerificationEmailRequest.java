package vn.edu.learningai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResendVerificationEmailRequest {
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;
}
