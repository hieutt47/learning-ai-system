package vn.edu.learningai.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ApiException extends RuntimeException{
    private final ErrorCode errorCode;
    /** Lỗi theo field (vd generation_id), trả trong {@code details}; null nếu không có. */
    private List<FieldError> details;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Lỗi validation gắn với một field cụ thể (tên snake_case như trên wire). */
    public static ApiException fieldError(String field, String message) {
        ApiException e = new ApiException(ErrorCode.VALIDATION_ERROR);
        e.details = List.of(new FieldError(field, message));
        return e;
    }
}
