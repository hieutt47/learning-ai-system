package vn.edu.learningai.ai;

import org.springframework.stereotype.Component;
import vn.edu.learningai.common.enums.Difficulty;

/**
 * Tạo prompt gửi AIClient. Ba dòng đầu (TASK/COUNT/DIFFICULTY) là header cố định: model đọc như yêu cầu,
 * MockAIClient đọc để sinh đúng loại và số lượng. Đổi nội dung prompt thì tăng *_PROMPT_VERSION
 * (ghi vào ai_generations.prompt_version để tra vết).
 */
@Component
public class AiPromptBuilder {

    public static final String QUIZ_PROMPT_VERSION = "quiz-v1";
    public static final String FLASHCARD_PROMPT_VERSION = "flashcard-v1";

    public String buildQuizPrompt(String sourceText, int count, Difficulty difficulty) {
        return """
            TASK: QUIZ
            COUNT: %d
            DIFFICULTY: %s

            Write exactly %d multiple-choice questions that test understanding of the lesson in <source>.
            Difficulty %s: EASY = recall of facts stated in the lesson, MEDIUM = understanding and simple \
            application, HARD = analysis or combining several ideas.
            Rules:
            - Use only information from the lesson. Write in the same language as the lesson.
            - Each question has 2 to 6 options and exactly one option with "is_correct": true.
            - No duplicate questions, no duplicate options within a question, no empty text.
            - Question content at most 4000 characters, each option at most 1000 characters.
            - "explanation" briefly says why the correct option is right.
            Output exactly this JSON shape and nothing else:
            {"questions":[{"content":"...","options":[{"content":"...","is_correct":true},\
            {"content":"...","is_correct":false}],"explanation":"..."}]}

            <source>
            %s
            </source>
            """.formatted(count, difficulty, count, difficulty, sanitize(sourceText));
    }

    public String buildFlashcardPrompt(String sourceText, int count) {
        return """
            TASK: FLASHCARD
            COUNT: %d

            Write exactly %d flashcards for the key ideas of the lesson in <source>.
            Rules:
            - Use only information from the lesson. Write in the same language as the lesson.
            - "front_text" is a short question or term, "back_text" is a concise answer or definition.
            - No duplicate cards, no empty text, each side at most 4000 characters.
            Output exactly this JSON shape and nothing else:
            {"flashcards":[{"front_text":"...","back_text":"..."}]}

            <source>
            %s
            </source>
            """.formatted(count, count, sanitize(sourceText));
    }

    /** Không để nội dung bài học tự đóng thẻ source (prompt injection đơn giản). */
    private static String sanitize(String sourceText) {
        return sourceText.replace("</source>", "</ source>").replace("<source>", "< source>");
    }
}
