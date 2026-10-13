package vn.edu.learningai.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    public static ErrorResponse from(String code, String message, List<FieldError> details)
    {
        return new ErrorResponse(code, message, details);
    }

    public static ErrorResponse from(String code, String message)
    {
        return new ErrorResponse(code, message, null);
    }


}
