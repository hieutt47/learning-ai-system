package vn.edu.learningai.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ApiException extends RuntimeException{
    private ErrorCode errorCode;
    private List<FieldError> details;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Lỗi validation gắn với một field cụ thể, trả trong {@code details}. */
    public static ApiException fieldError(String field, String message) {
        ApiException e = new ApiException(ErrorCode.VALIDATION_ERROR);
        e.details = List.of(new FieldError(field, message));
        return e;
    }
}
