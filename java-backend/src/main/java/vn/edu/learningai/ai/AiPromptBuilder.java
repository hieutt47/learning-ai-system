package vn.edu.learningai.ai;

import org.springframework.stereotype.Component;
import vn.edu.learningai.common.enums.Difficulty;

import java.util.List;

/**
 * Tạo prompt gửi AIClient. Ba dòng đầu (TASK/COUNT/DIFFICULTY) là header cố định: model đọc như yêu cầu,
 * MockAIClient đọc để sinh đúng loại và số lượng. Đổi nội dung prompt thì tăng *_PROMPT_VERSION
 * (ghi vào ai_generations.prompt_version để tra vết).
 */
@Component
public class AiPromptBuilder {

    public static final String QUIZ_PROMPT_VERSION = "quiz-v2";
    public static final String FLASHCARD_PROMPT_VERSION = "flashcard-v2";

    public String buildQuizPrompt(String sourceText, int count, Difficulty difficulty) {
        return """
            TASK: QUIZ
            COUNT: %d
            DIFFICULTY: %s

            Write exactly %d multiple-choice questions testing understanding of key ideas of the lesson in <source>.
            Target difficulty %s:
            - EASY: Recall directly stated facts, definitions, and core terminology from the lesson.
            - MEDIUM: Understanding and application; explain reasons, compare concepts, or solve simple practical scenarios.
            - HARD: Deep analysis, evaluation, combining multiple concepts, identifying misconceptions, or troubleshooting complex scenarios.

            Rules:
            - Use ONLY information explicitly covered in the lesson. Write in the same language as the lesson.
            - Distribute questions across the entire lesson content rather than focusing only on the beginning.
            - Questions must focus on meaningful learning objectives. DO NOT ask trivial details (such as slide numbers, video timestamps, author name, file formats).
            - Each question has 2 to 6 options and exactly one option with "is_correct": true.
            - Distractors (incorrect options) must be plausible, relevant, and of similar length to the correct answer. NEVER use "All of the above" or "None of the above".
            - No duplicate questions, no duplicate options within a question, no empty text.
            - Keep question content concise and within 4000 characters, each option within 1000 characters.
            - "explanation" briefly cites the rationale from the lesson in 1-2 sentences explaining why the correct option is right.
            Output exactly this JSON shape and nothing else:
            {"questions":[{"content":"...","options":[{"content":"...","is_correct":true},\
            {"content":"...","is_correct":false}],"explanation":"..."}]}

            <source>
            %s
            </source>
            """.formatted(count, difficulty, count, difficulty, sanitize(sourceText));
    }

    public String buildFlashcardPrompt(String sourceText, int count) {
        return buildFlashcardPrompt(sourceText, count, List.of());
    }

    public String buildFlashcardPrompt(String sourceText, int count, List<String> existingFrontTexts) {
        String exclusionBlock = buildExclusionBlock(existingFrontTexts);

        return """
            TASK: FLASHCARD
            COUNT: %d

            Write exactly %d flashcards for key terms, definitions, and essential concepts of the lesson in <source>.
            %s
            Rules:
            - Use ONLY information from the lesson. Write in the same language as the lesson.
            - Distribute flashcards across the entire lesson content rather than focusing only on the beginning.
            - "front_text" must be a concise prompt, question, or term (1 sentence, ideally under 120 characters).
            - "back_text" must be a clear, succinct explanation or definition (1 to 2 sentences).
            - Focus on atomic knowledge (one idea per card).
            - No duplicate cards, no empty text, each side at most 4000 characters.
            Output exactly this JSON shape and nothing else:
            {"flashcards":[{"front_text":"...","back_text":"..."}]}

            <source>
            %s
            </source>
            """.formatted(count, count, exclusionBlock, sanitize(sourceText));
    }

    private static String buildExclusionBlock(List<String> existingFrontTexts) {
        if (existingFrontTexts == null || existingFrontTexts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(existingFrontTexts.size(), 30);
        for (int i = 0; i < limit; i++) {
            String raw = existingFrontTexts.get(i);
            if (raw == null) {
                continue;
            }
            String item = sanitize(raw).replaceAll("\\s+", " ").trim();
            if (!item.isEmpty()) {
                if (item.length() > 150) {
                    item = item.substring(0, 150) + "...";
                }
                if (sb.isEmpty()) {
                    sb.append("\nDo NOT duplicate or repeat the following concepts already created for this lesson:\n");
                }
                sb.append("- ").append(item).append("\n");
            }
        }
        return sb.toString();
    }

    /** Không để nội dung bài học tự đóng thẻ source (prompt injection đơn giản). */
    private static String sanitize(String sourceText) {
        return sourceText.replace("</source>", "</ source>").replace("<source>", "< source>");
    }
}
