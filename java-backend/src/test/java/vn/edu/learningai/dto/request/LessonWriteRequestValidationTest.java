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
        assertTrue(validator.validate(request("https://www.youtube-nocookie.com/embed/eIrMbAQSU34")).isEmpty());
        assertTrue(validator.validate(request("https://youtube.com/shorts/eIrMbAQSU34")).isEmpty());
        assertTrue(validator.validate(request(" ")).isEmpty());
        assertTrue(validator.validate(request(null)).isEmpty());
    }

    @Test
    void rejectsNonYouTubeMalformedIdsAndHtmlIframe() {
        assertInvalid("https://example.com/watch?v=eIrMbAQSU34");
        assertInvalid("javascript:alert(1)");
        assertInvalid("https://www.youtube.com/watch");
        assertInvalid("https://www.youtube.com/watch?v=   ");
        assertInvalid("https://youtu.be/");
        assertInvalid("https://youtu.be/eIrMbAQSU34/extra");
        assertInvalid("https://youtube.com/embed/");
        assertInvalid("https://youtube.com/shorts/not-valid!!!");
        assertInvalid("<iframe src=\"https://www.youtube.com/embed/eIrMbAQSU34\"></iframe>");
    }

    @Test
    void transcriptMustFitMySqlTextColumnInUtf8Bytes() {
        assertTrue(validator.validate(new LessonWriteRequest("Bài 1", "", "a".repeat(65535))).isEmpty());
        var violations = validator.validate(new LessonWriteRequest("Bài 1", "", "😀".repeat(16384)));
        assertEquals(1, violations.size());
        assertEquals("transcriptText", violations.iterator().next().getPropertyPath().toString());
    }

    private void assertInvalid(String videoUrl) {
        var violations = validator.validate(request(videoUrl));
        assertEquals(1, violations.size(), "Expected invalid videoUrl: " + videoUrl);
        assertEquals("videoUrl", violations.iterator().next().getPropertyPath().toString());
    }

    private LessonWriteRequest request(String videoUrl) {
        return new LessonWriteRequest("Bài 1", videoUrl, "Nội dung");
    }
}
