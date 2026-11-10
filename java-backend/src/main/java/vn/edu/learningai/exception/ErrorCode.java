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
    ENROLLMENT_REQUIRED(HttpStatus.FORBIDDEN, "Course enrollment is required"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INVALID_STATE(HttpStatus.CONFLICT, "Invalid state for this operation"),
    INVALID_QUIZ(HttpStatus.CONFLICT, "Quiz content is invalid for publication"),
    RESOURCE_IN_USE(HttpStatus.CONFLICT, "Resource is in use"),

    COURSE_NOT_FREE(HttpStatus.CONFLICT, "Only free courses can be enrolled"),
    LESSON_DELETE_BLOCKED(HttpStatus.CONFLICT, "Lesson has learning history and cannot be deleted"),

    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type"),
    AI_OUTPUT_INVALID(HttpStatus.BAD_GATEWAY, "AI output is invalid"),
    AI_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "AI provider error"),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI provider timed out"),
    SOURCE_TEXT_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Source text is empty"),
    STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store or read the file"),
    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error"),

    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists");

    private final HttpStatus status;
    private final String defaultMessage;
}
