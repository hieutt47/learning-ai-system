package vn.edu.learningai.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordRequestTest {
    @Test
    void acceptsLegacyCurrentPasswordAndNewPasswordBoundaries() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new ChangePasswordRequest("old", "x".repeat(10)))).isEmpty();
            assertThat(factory.getValidator().validate(new ChangePasswordRequest("old", "x".repeat(72)))).isEmpty();
        }
    }

    @Test
    void rejectsMissingBlankAndOverlongPasswords() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new ChangePasswordRequest(null, null))).hasSize(2);
            assertThat(factory.getValidator().validate(new ChangePasswordRequest(" ", " ".repeat(10)))).hasSize(2);
            assertThat(factory.getValidator().validate(new ChangePasswordRequest("x".repeat(73), "x".repeat(73)))).hasSize(2);
            assertThat(factory.getValidator().validate(new ChangePasswordRequest("old", "x".repeat(9)))).hasSize(1);
        }
    }
}
