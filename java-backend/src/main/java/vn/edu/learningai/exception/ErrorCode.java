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
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    ENROLLMENT_REQUIRED(HttpStatus.FORBIDDEN, "Course enrollment required"),
    INVALID_STATE(HttpStatus.CONFLICT, "Operation is not allowed in the current state"),
    LESSON_DELETE_BLOCKED(HttpStatus.CONFLICT, "Lesson has learning history and cannot be deleted"),
    SOURCE_TEXT_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Source text is empty"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type"),
    STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store or read the file"),

    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error");

    private final HttpStatus status;
    private final String defaultMessage;
}
