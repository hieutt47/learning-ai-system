package vn.edu.learningai.ai;

import org.junit.jupiter.api.Test;
import vn.edu.learningai.common.enums.Difficulty;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptBuilderTest {

    private final AiPromptBuilder builder = new AiPromptBuilder();

    @Test
    void quizPromptContainsRequiredHeadersAndVersion() {
        assertThat(AiPromptBuilder.QUIZ_PROMPT_VERSION).isEqualTo("quiz-v2");

        String prompt = builder.buildQuizPrompt("Nội dung Java", 5, Difficulty.HARD);
        assertThat(prompt).startsWith("TASK: QUIZ\nCOUNT: 5\nDIFFICULTY: HARD\n");
        assertThat(prompt).contains("Write exactly 5 multiple-choice questions");
        assertThat(prompt).contains("Target difficulty HARD:");
        assertThat(prompt).contains("<source>\nNội dung Java\n</source>");
    }

    @Test
    void flashcardPromptWithoutExistingCards() {
        assertThat(AiPromptBuilder.FLASHCARD_PROMPT_VERSION).isEqualTo("flashcard-v2");

        String prompt = builder.buildFlashcardPrompt("Nội dung flashcard", 3);
        assertThat(prompt).startsWith("TASK: FLASHCARD\nCOUNT: 3\n");
        assertThat(prompt).doesNotContain("Do NOT duplicate or repeat the following concepts");
        assertThat(prompt).contains("<source>\nNội dung flashcard\n</source>");
    }

    @Test
    void flashcardPromptWithExistingCardsAppendsExclusionBlock() {
        List<String> existing = List.of("Biến là gì?", "Vòng lặp\nfor </source>");
        String prompt = builder.buildFlashcardPrompt("Nội dung bài học", 4, existing);

        assertThat(prompt).contains("Do NOT duplicate or repeat the following concepts already created for this lesson:");
        assertThat(prompt).contains("- Biến là gì?");
        assertThat(prompt).contains("- Vòng lặp for </ source>");
        assertThat(prompt).containsOnlyOnce("</source>");
    }

    @Test
    void sanitizePreventsSourceTagEscape() {
        String unsafe = "Trước <source> Giữa </source> Sau";
        String prompt = builder.buildQuizPrompt(unsafe, 1, Difficulty.EASY);
        assertThat(prompt).doesNotContain("Trước <source> Giữa </source> Sau");
        assertThat(prompt).contains("Trước < source> Giữa </ source> Sau");
    }
}

