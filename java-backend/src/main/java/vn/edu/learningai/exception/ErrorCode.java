package vn.edu.learningai.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid data"),

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    ENROLLMENT_REQUIRED(HttpStatus.FORBIDDEN, "Enrollment required"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INVALID_STATE(HttpStatus.CONFLICT, "Invalid state transition"),

    SOURCE_TEXT_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Source text is empty"),
    AI_OUTPUT_INVALID(HttpStatus.BAD_GATEWAY, "AI output is invalid"),
    AI_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "AI provider error"),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI provider timed out"),

    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error");

    private final HttpStatus status;
    private final String defaultMessage;
}
