package vn.edu.learningai.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.JsonNode;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

import java.util.Set;

public record CreateQuizRequest(String title, Long generationId) {
    private static final Set<String> FIELDS = Set.of("title", "generation_id");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CreateQuizRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalid("Request body must be an object");
        }
        for (String field : body.propertyNames()) {
            if (!FIELDS.contains(field)) {
                throw invalid("Unknown field: " + field);
            }
        }

        JsonNode title = body.get("title");
        if (title == null || !title.isString() || title.stringValue().isBlank()
            || title.stringValue().codePointCount(0, title.stringValue().length()) > 200) {
            throw invalid("title must be a non-blank string of at most 200 characters");
        }

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
        return new CreateQuizRequest(title.stringValue(), generationId);
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message);
    }
}
