package vn.edu.learningai.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid data"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    ACCOUNT_INACTIVE(HttpStatus.UNAUTHORIZED, "Account is inactive"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Not allowed"),
    ENROLLMENT_REQUIRED(HttpStatus.FORBIDDEN, "Course enrollment required"),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "User not found"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INVALID_STATE(HttpStatus.CONFLICT, "Invalid state"),
    COURSE_NOT_FREE(HttpStatus.CONFLICT, "Only free courses can be enrolled"),
    RESOURCE_IN_USE(HttpStatus.CONFLICT, "Resource is in use"),
    LESSON_DELETE_BLOCKED(HttpStatus.CONFLICT, "Lesson has learning history and cannot be deleted"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type"),
    SOURCE_TEXT_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Source text is empty"),
    STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store or read the file"),
    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Uncategorized error");
    private final HttpStatus status;
    private final String defaultMessage;
}
