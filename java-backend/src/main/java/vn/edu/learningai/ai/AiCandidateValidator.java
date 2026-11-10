package vn.edu.learningai.ai;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.learningai.dto.response.CardCandidate;
import vn.edu.learningai.dto.response.OptionCandidate;
import vn.edu.learningai.dto.response.QuestionCandidate;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kiểm tra output AI theo contracts/ai-output.md: đúng object/kiểu/field bắt buộc, không null/rỗng,
 * đúng số lượng, không trùng sau normalize, đúng giới hạn độ dài và số option, đúng một is_correct=true,
 * từ chối field lạ. Không sửa output: sai là ném {@link InvalidCandidateException}.
 * {@link #checkQuestion} và các hằng số giới hạn dùng chung được cho phần quiz (người 4).
 */
@Component
public class AiCandidateValidator {

    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 6;
    public static final int MAX_QUESTION_LENGTH = 4000;
    public static final int MAX_OPTION_LENGTH = 1000;
    public static final int MAX_CARD_SIDE_LENGTH = 4000;

    private static final Set<String> QUIZ_ROOT = Set.of("questions");
    private static final Set<String> QUESTION_FIELDS = Set.of("content", "options", "explanation");
    private static final Set<String> OPTION_FIELDS = Set.of("content", "is_correct");
    private static final Set<String> CARD_ROOT = Set.of("flashcards");
    private static final Set<String> CARD_FIELDS = Set.of("front_text", "back_text");

    private final JsonMapper jsonMapper;

    public AiCandidateValidator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public List<QuestionCandidate> parseQuizCandidate(String raw, int count) {
        JsonNode root = parseRoot(raw, QUIZ_ROOT);
        JsonNode items = requireArray(root, "questions", count);

        List<QuestionCandidate> questions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            String path = "questions[" + i + "]";
            JsonNode q = requireObject(items.get(i), path, QUESTION_FIELDS);
            String content = requireText(q, "content", path, MAX_QUESTION_LENGTH);
            String explanation = optionalText(q, "explanation", path, MAX_QUESTION_LENGTH);

            JsonNode optionNodes = q.get("options");
            if (optionNodes == null || !optionNodes.isArray()) {
                throw invalid(path + ".options must be an array");
            }
            List<OptionCandidate> options = new ArrayList<>();
            for (int j = 0; j < optionNodes.size(); j++) {
                String optionPath = path + ".options[" + j + "]";
                JsonNode o = requireObject(optionNodes.get(j), optionPath, OPTION_FIELDS);
                String optionContent = requireText(o, "content", optionPath, MAX_OPTION_LENGTH);
                JsonNode correct = o.get("is_correct");
                if (correct == null || !correct.isBoolean()) {
                    throw invalid(optionPath + ".is_correct must be a boolean");
                }
                options.add(new OptionCandidate(optionContent, correct.booleanValue()));
            }
            checkQuestion(content, options, path);

            if (!seen.add(normalize(content))) {
                throw invalid(path + " duplicates an earlier question");
            }
            questions.add(new QuestionCandidate(content, options, explanation));
        }
        return questions;
    }

    public List<CardCandidate> parseCardCandidate(String raw, int count) {
        JsonNode root = parseRoot(raw, CARD_ROOT);
        JsonNode items = requireArray(root, "flashcards", count);

        List<CardCandidate> cards = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            String path = "flashcards[" + i + "]";
            JsonNode c = requireObject(items.get(i), path, CARD_FIELDS);
            String front = requireText(c, "front_text", path, MAX_CARD_SIDE_LENGTH);
            String back = requireText(c, "back_text", path, MAX_CARD_SIDE_LENGTH);
            if (!seen.add(normalize(front))) {
                throw invalid(path + " duplicates an earlier card");
            }
            cards.add(new CardCandidate(front, back));
        }
        return cards;
    }

    /** Quy tắc một câu hỏi trắc nghiệm: 2–6 option, đúng 1 đáp án đúng, option không trùng. */
    public void checkQuestion(String content, List<OptionCandidate> options, String path) {
        if (content == null || content.isBlank() || content.length() > MAX_QUESTION_LENGTH) {
            throw invalid(path + ".content must be 1-" + MAX_QUESTION_LENGTH + " characters");
        }
        if (options == null || options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
            throw invalid(path + " must have " + MIN_OPTIONS + "-" + MAX_OPTIONS + " options");
        }
        long correct = options.stream().filter(o -> Boolean.TRUE.equals(o.isCorrect())).count();
        if (correct != 1) {
            throw invalid(path + " must have exactly one correct option, found " + correct);
        }
        Set<String> seen = new HashSet<>();
        for (OptionCandidate o : options) {
            if (o.content() == null || o.content().isBlank() || o.content().length() > MAX_OPTION_LENGTH) {
                throw invalid(path + " option content must be 1-" + MAX_OPTION_LENGTH + " characters");
            }
            if (!seen.add(normalize(o.content()))) {
                throw invalid(path + " has duplicate options");
            }
        }
    }

    private JsonNode parseRoot(String raw, Set<String> allowedFields) {
        if (raw == null || raw.isBlank()) {
            throw invalid("empty output");
        }
        JsonNode root;
        try {
            root = jsonMapper.readTree(stripCodeFence(raw.strip()));
        } catch (JacksonException e) {
            throw invalid("output is not valid JSON");
        }
        return requireObject(root, "root", allowedFields);
    }

    /** Chấp nhận đúng một khối ```json ... ``` bao quanh toàn bộ output; markdown khác coi là lỗi parse. */
    private static String stripCodeFence(String s) {
        if (s.startsWith("```") && s.endsWith("```") && s.length() > 6) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                return s.substring(firstNewline + 1, s.length() - 3).strip();
            }
        }
        return s;
    }

    private static JsonNode requireArray(JsonNode root, String field, int count) {
        JsonNode items = root.get(field);
        if (items == null || !items.isArray()) {
            throw invalid(field + " must be an array");
        }
        if (items.size() != count) {
            throw invalid(field + " must contain exactly " + count + " items, got " + items.size());
        }
        return items;
    }

    private static JsonNode requireObject(JsonNode node, String path, Set<String> allowedFields) {
        if (node == null || !node.isObject()) {
            throw invalid(path + " must be an object");
        }
        for (String name : node.propertyNames()) {
            if (!allowedFields.contains(name)) {
                throw invalid(path + " has unexpected field '" + name + "'");
            }
        }
        return node;
    }

    private static String requireText(JsonNode node, String field, String path, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            throw invalid(path + "." + field + " must be a string");
        }
        String text = value.stringValue().strip();
        if (text.isEmpty() || text.length() > maxLength) {
            throw invalid(path + "." + field + " must be 1-" + maxLength + " characters");
        }
        return text;
    }

    private static String optionalText(JsonNode node, String field, String path, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw invalid(path + "." + field + " must be a string or null");
        }
        String text = value.stringValue().strip();
        if (text.length() > maxLength) {
            throw invalid(path + "." + field + " must be at most " + maxLength + " characters");
        }
        return text.isEmpty() ? null : text;
    }

    /** Normalize để so trùng: Unicode NFC, chữ thường, gộp khoảng trắng. */
    static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC)
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .strip();
    }

    private static InvalidCandidateException invalid(String reason) {
        return new InvalidCandidateException(reason);
    }

    /** Output AI sai contract; service lưu generation FAILED + AI_OUTPUT_INVALID. */
    public static class InvalidCandidateException extends RuntimeException {
        public InvalidCandidateException(String reason) {
            super(reason);
        }
    }
}
