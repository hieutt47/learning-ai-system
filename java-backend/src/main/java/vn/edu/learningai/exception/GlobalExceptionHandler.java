package vn.edu.learningai.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategy;
import vn.edu.learningai.config.RequestIdFilter;
import vn.edu.learningai.storage.StorageException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final ObjectMapper objectMapper;
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest request) {
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(errorBody(e.getErrorCode(), e.getMessage(), null, request));
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        List<FieldError> fieldErrors = new ArrayList<>();
        PropertyNamingStrategy namingStrategy = objectMapper.deserializationConfig().getPropertyNamingStrategy();
        for (org.springframework.validation.FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            if (namingStrategy != null) {
                String translated = namingStrategy.nameForField(
                    objectMapper.deserializationConfig(), null, field);
                field = translated.replace("\"", "");
            }
            fieldErrors.add(new FieldError(field, fieldError.getDefaultMessage()));
        }
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(errorBody(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                fieldErrors, request));
    }
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
        MissingServletRequestPartException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(errorBody(ErrorCode.VALIDATION_ERROR, "Required part '" + e.getRequestPartName() + "' is missing",
                null, request));
    }
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(errorBody(ErrorCode.VALIDATION_ERROR,
                "Request content type is not supported; send multipart/form-data", null, request));
    }
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception e,
        Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        if (statusCode.is5xxServerError()) {
            log.error("Unhandled request failure", e);
        }
        ErrorCode code = codeFor(statusCode);
        return ResponseEntity
            .status(statusCode)
            .headers(headers)
            .body(errorBody(code, messageFor(e, code), null, request));
    }
    private ErrorCode codeFor(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 404 -> ErrorCode.NOT_FOUND;
            case 413 -> ErrorCode.FILE_TOO_LARGE;
            default -> statusCode.is4xxClientError()
                ? ErrorCode.VALIDATION_ERROR
                : ErrorCode.UNCATEGORIZED_EXCEPTION;
        };
    }
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException e, HttpServletRequest request) {
        log.error("Storage failure", e);
        return ResponseEntity
            .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatus())
            .body(errorBody(ErrorCode.UNCATEGORIZED_EXCEPTION, ErrorCode.UNCATEGORIZED_EXCEPTION.getDefaultMessage(),
                null, request));
    }
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(errorBody(ErrorCode.VALIDATION_ERROR, "Request body is invalid", null, request));
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
        MaxUploadSizeExceededException e,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        return ResponseEntity
            .status(ErrorCode.FILE_TOO_LARGE.getStatus())
            .body(errorBody(ErrorCode.FILE_TOO_LARGE, "Maximum upload size exceeded", null, request));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestParameter(
        MethodArgumentTypeMismatchException e,
        HttpServletRequest request
    ) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(errorBody(ErrorCode.VALIDATION_ERROR, "Invalid request parameter",
                List.of(new FieldError(e.getName(), "Invalid value")), request));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("Unhandled request failure", e);
        return ResponseEntity
            .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatus())
            .body(errorBody(ErrorCode.UNCATEGORIZED_EXCEPTION, ErrorCode.UNCATEGORIZED_EXCEPTION.getDefaultMessage(),
                null, request));
    }
    private String messageFor(Exception e, ErrorCode code) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return code.getDefaultMessage();
        }
        return e.getMessage();
    }

    private ErrorResponse errorBody(ErrorCode code, String message, List<FieldError> details, WebRequest request) {
        return ErrorResponse.from(code.name(), message, details, requestId(request));
    }
    private ErrorResponse errorBody(ErrorCode code, String message, List<FieldError> details, HttpServletRequest request) {
        return ErrorResponse.from(code.name(), message, details,
            (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
    private String requestId(WebRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
        return value instanceof String id ? id : null;
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.getStatus())
            .body(ErrorResponse.from(
                ErrorCode.UNAUTHENTICATED.name(),
                ErrorCode.UNAUTHENTICATED.getDefaultMessage()));
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatus())
            .body(ErrorResponse.from(
                ErrorCode.FORBIDDEN.name(), ErrorCode.FORBIDDEN.getDefaultMessage()));
    }
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException e) {

        return ResponseEntity.status(ErrorCode.ACCOUNT_INACTIVE.getStatus())
            .body(ErrorResponse.from(
                ErrorCode.ACCOUNT_INACTIVE.name(),
                ErrorCode.ACCOUNT_INACTIVE.getDefaultMessage()));
    }
}
