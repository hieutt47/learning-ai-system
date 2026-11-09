package vn.edu.learningai.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonWriteRequestValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsSupportedYouTubeUrlsAndOptionalBlankValue() {
        assertTrue(validator.validate(request("https://www.youtube.com/watch?v=eIrMbAQSU34")).isEmpty());
        assertTrue(validator.validate(request("https://youtu.be/eIrMbAQSU34")).isEmpty());
        assertTrue(validator.validate(request("https://youtube.com/embed/eIrMbAQSU34")).isEmpty());
        assertTrue(validator.validate(request("https://youtube.com/shorts/eIrMbAQSU34")).isEmpty());
        assertTrue(validator.validate(request(" ")).isEmpty());
        assertTrue(validator.validate(request(null)).isEmpty());
    }

    @Test
    void rejectsNonYouTubeAndIncompleteUrls() {
        assertInvalid("https://example.com/watch?v=eIrMbAQSU34");
        assertInvalid("javascript:alert(1)");
        assertInvalid("https://www.youtube.com/watch");
        assertInvalid("https://youtu.be/");
    }

    private void assertInvalid(String videoUrl) {
        var violations = validator.validate(request(videoUrl));
        assertEquals(1, violations.size());
        assertEquals("videoUrl", violations.iterator().next().getPropertyPath().toString());
    }

    private LessonWriteRequest request(String videoUrl) {
        return new LessonWriteRequest("Bài 1", videoUrl, "Nội dung");
    }
}
