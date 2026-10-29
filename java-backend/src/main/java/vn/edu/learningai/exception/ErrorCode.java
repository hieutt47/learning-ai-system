package vn.edu.learningai.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid data"),

    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "Account is inactive"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),

    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error"),

    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "User not found"),
    USER_EXISTED(HttpStatus.BAD_REQUEST, "User existed"),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "User is inactive"),

    REFRESH_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "Refresh token not found"),

    REFRESH_TOKEN_IS_NULL(HttpStatus.BAD_REQUEST, "Refresh token is null");

    private final HttpStatus status;
    private final String defaultMessage;
}
