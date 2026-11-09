package vn.edu.learningai.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.JsonNode;
import vn.edu.learningai.common.enums.QuizStatus;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;

public record QuizPublicationRequest(QuizStatus status) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static QuizPublicationRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("status")) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Only status is allowed and required");
        }
        JsonNode status = body.get("status");
        if (!status.isString() || !("PUBLISHED".equals(status.stringValue()) || "CLOSED".equals(status.stringValue()))) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "status must be PUBLISHED or CLOSED");
        }
        return new QuizPublicationRequest(QuizStatus.valueOf(status.stringValue()));
    }
}
