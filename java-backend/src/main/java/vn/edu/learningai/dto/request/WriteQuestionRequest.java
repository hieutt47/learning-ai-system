package vn.edu.learningai.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.JsonNode;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public record WriteQuestionRequest(String content, String explanation, Integer orderIndex,
                                   List<Option> options, Long generationId) {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WriteQuestionRequest fromJson(JsonNode body) {
        fields(body, Set.of("content", "explanation", "order_index", "options", "generation_id"));
        String content = text(body.get("content"), "content", 4000, false);
        if (!body.has("explanation")) {
            throw invalid("explanation is required");
        }
        String explanation = text(body.get("explanation"), "explanation", 4000, true);
        int orderIndex = position(body.get("order_index"));
        JsonNode options = body.get("options");
        if (options == null || !options.isArray() || options.size() < 2 || options.size() > 6) {
            throw invalid("options must contain 2 to 6 items");
        }
        List<Option> values = StreamSupport.stream(options.spliterator(), false).map(option -> {
            fields(option, Set.of("content", "is_correct", "order_index"));
            JsonNode correct = option.get("is_correct");
            if (correct == null || !correct.isBoolean()) {
                throw invalid("is_correct must be a boolean");
            }
            return new Option(text(option.get("content"), "options.content", 1000, false),
                correct.booleanValue(), position(option.get("order_index")));
        }).toList();
        JsonNode generation = body.get("generation_id");
        Long generationId = null;
        if (generation != null && !generation.isNull()) {
            if (!generation.isString() || !generation.stringValue().matches("[1-9][0-9]{0,18}")) {
                throw invalid("generation_id must be a positive BIGINT string");
            }
            try {
                generationId = Long.valueOf(generation.stringValue());
            } catch (NumberFormatException e) {
                throw invalid("generation_id exceeds the signed BIGINT range");
            }
        }
        var request = new WriteQuestionRequest(content, explanation, orderIndex, values, generationId);
        request.validate();
        return request;
    }

    public void validate() {
        if (!validText(content, 4000) || explanation != null && length(explanation) > 4000
            || orderIndex == null || orderIndex < 0 || options == null || options.size() < 2 || options.size() > 6) {
            throw invalid("Invalid question content or options");
        }
        Set<String> contents = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        int correctCount = 0;
        for (Option option : options) {
            if (option == null || !validText(option.content(), 1000) || option.isCorrect() == null
                || option.orderIndex() == null || option.orderIndex() < 0) {
                throw invalid("Invalid option");
            }
            String normalized = WHITESPACE.matcher(Normalizer.normalize(option.content(), Normalizer.Form.NFC))
                .replaceAll(" ").strip().toLowerCase(Locale.ROOT);
            if (!contents.add(normalized)) {
                throw invalid("Option content must be unique after normalization");
            }
            if (!positions.add(option.orderIndex())) {
                throw new ApiException(ErrorCode.INVALID_STATE, "Option order_index must be unique");
            }
            if (option.isCorrect()) {
                correctCount++;
            }
        }
        if (correctCount != 1) {
            throw invalid("Exactly one option must be correct");
        }
    }

    private static void fields(JsonNode body, Set<String> allowed) {
        if (body == null || !body.isObject()) {
            throw invalid("Expected an object");
        }
        for (String field : body.propertyNames()) {
            if (!allowed.contains(field)) {
                throw invalid("Unknown field: " + field);
            }
        }
    }

    private static String text(JsonNode value, String field, int max, boolean nullable) {
        if (value != null && value.isNull() && nullable) {
            return null;
        }
        if (value == null || !value.isString() || length(value.stringValue()) > max
            || !nullable && !validText(value.stringValue(), max)) {
            throw invalid("Invalid " + field);
        }
        return value.stringValue();
    }

    private static boolean validText(String value, int max) {
        return value != null && !WHITESPACE.matcher(value).replaceAll("").isEmpty() && length(value) <= max;
    }

    private static int length(String value) {
        return value.codePointCount(0, value.length());
    }

    private static int position(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0) {
            throw invalid("order_index must be a non-negative integer");
        }
        return value.intValue();
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message);
    }

    public record Option(String content, Boolean isCorrect, Integer orderIndex) {}
}
