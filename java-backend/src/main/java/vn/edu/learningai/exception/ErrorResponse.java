package vn.edu.learningai.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ErrorResponse {
    private String code;
    private String message;
    private List<FieldError> details;

    @JsonProperty("request_id")
    private String requestId;

    public static ErrorResponse from(String code, String message, List<FieldError> details, String requestId) {
        return new ErrorResponse(code, message, details, requestId);
    }

    public static ErrorResponse from(String code, String message, String requestId) {
        return new ErrorResponse(code, message, null, requestId);
    }

    /**
     * No request id available.
     *
     * <p>Used by handlers on paths that run outside the request chain (Spring
     * Security's entry point and access-denied handler fire before the filter
     * that assigns ids), so the field is simply omitted rather than fake.</p>
     */
    public static ErrorResponse from(String code, String message) {
        return new ErrorResponse(code, message, null, null);
    }

    /** Same, with field-level details. */
    public static ErrorResponse from(String code, String message, List<FieldError> details) {
        return new ErrorResponse(code, message, details, null);
    }
}
