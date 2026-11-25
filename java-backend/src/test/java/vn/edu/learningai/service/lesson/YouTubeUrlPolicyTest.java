package vn.edu.learningai.service.lesson;

import org.junit.jupiter.api.Test;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouTubeUrlPolicyTest {
    private final YouTubeUrlPolicy policy = new YouTubeUrlPolicy();

    @Test
    void normalizesSupportedYouTubeFormatsToCanonicalWatchUrl() {
        assertThat(policy.normalizeOptional("  https://youtu.be/eIrMbAQSU34?si=abc  "))
            .isEqualTo("https://www.youtube.com/watch?v=eIrMbAQSU34");
        assertThat(policy.normalizeOptional("https://m.youtube.com/watch?v=eIrMbAQSU34&t=12s"))
            .isEqualTo("https://www.youtube.com/watch?v=eIrMbAQSU34");
        assertThat(policy.normalizeOptional("https://www.youtube-nocookie.com/embed/eIrMbAQSU34"))
            .isEqualTo("https://www.youtube.com/watch?v=eIrMbAQSU34");
        assertThat(policy.normalizeOptional("https://youtube.com/shorts/secret-a1/"))
            .isEqualTo("https://www.youtube.com/watch?v=secret-a1");
        assertThat(policy.normalizeOptional("   ")).isEmpty();
        assertThat(policy.normalizeOptional(null)).isEmpty();
    }

    @Test
    void rejectsExtraPathSegmentsInvalidIdsAndIframeHtml() {
        assertThat(YouTubeUrlPolicy.isValidOptional("https://youtu.be/eIrMbAQSU34/extra")).isFalse();
        assertThat(YouTubeUrlPolicy.isValidOptional("https://www.youtube.com/embed/")).isFalse();
        assertThat(YouTubeUrlPolicy.isValidOptional("https://www.youtube.com/watch?v=bad!id")).isFalse();
        assertThat(YouTubeUrlPolicy.isValidOptional("<iframe src='https://youtu.be/eIrMbAQSU34'></iframe>")).isFalse();

        assertThatThrownBy(() -> policy.normalizeOptional("https://evil.example/watch?v=eIrMbAQSU34"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
