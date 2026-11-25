package vn.edu.learningai.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceSamplerTest {

    @Test
    void shortTextIsNotTruncated() {
        String text = "Nội dung bài học ngắn.";
        SourceSampler.SampleResult result = SourceSampler.sample(text, 100, "spread");
        assertThat(result.truncated()).isFalse();
        assertThat(result.text()).isEqualTo(text);
    }

    @Test
    void headStrategyTruncatesAtBudget() {
        String text = "0123456789ABCDEF";
        SourceSampler.SampleResult result = SourceSampler.sample(text, 10, "head");
        assertThat(result.truncated()).isTrue();
        assertThat(result.text()).isEqualTo("0123456789");
    }

    @Test
    void safeTruncateDoesNotSplitSurrogatePair() {
        String emojiText = "ab😀cd";
        // 😀 chiếm 2 code units: charAt(2) là high surrogate, charAt(3) là low surrogate
        String truncated = SourceSampler.safeTruncate(emojiText, 3);
        assertThat(truncated).isEqualTo("ab");
    }

    @Test
    void spreadStrategyRetainsHeadAndTailAndStaysWithinBudget() {
        StringBuilder sb = new StringBuilder();
        sb.append("Đoạn mở đầu: Giới thiệu kiến thức cơ bản về Java.\n\n");
        for (int i = 1; i <= 20; i++) {
            sb.append("Phần ").append(i).append(": Nội dung chi tiết của phần học số ").append(i)
              .append(". Đây là kiến thức chuyên sâu về lập trình hướng đối tượng, đa hình và kế thừa.\n\n");
        }
        sb.append("Đoạn kết luận: Tóm tắt toàn bộ bài học và bài tập về nhà.");

        String fullText = sb.toString();
        int budget = 500;

        SourceSampler.SampleResult result = SourceSampler.sample(fullText, budget, "spread");

        assertThat(result.truncated()).isTrue();
        assertThat(result.text().length()).isLessThanOrEqualTo(budget);
        assertThat(result.text()).startsWith("Đoạn mở đầu:");
        assertThat(result.text()).endsWith("Tóm tắt toàn bộ bài học và bài tập về nhà.");
        assertThat(result.text()).contains(SourceSampler.SEPARATOR);
    }

    @Test
    void spreadStrategyFallsBackCleanlyWhenBlocksAreFew() {
        String text = "Đoạn một rất dài vượt quá ngân sách.\n\nĐoạn hai cũng rất dài.";
        SourceSampler.SampleResult result = SourceSampler.sample(text, 20, "spread");
        assertThat(result.truncated()).isTrue();
        assertThat(result.text().length()).isLessThanOrEqualTo(20);
    }
}

