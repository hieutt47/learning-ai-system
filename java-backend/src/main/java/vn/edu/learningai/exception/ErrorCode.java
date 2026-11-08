package vn.edu.learningai.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid data"),

    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "Account is inactive"),

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required or credentials are invalid"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INVALID_STATE(HttpStatus.CONFLICT, "Invalid state for this operation"),

    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error"),

    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists");

    private final HttpStatus status;
    private final String defaultMessage;
}
