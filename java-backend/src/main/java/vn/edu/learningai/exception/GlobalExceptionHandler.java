package vn.edu.learningai.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategy;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final ObjectMapper objectMapper;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ErrorResponse.from(e.getErrorCode().name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {

        List<FieldError> fieldErrors = new ArrayList<>();
        PropertyNamingStrategy namingStrategy = objectMapper.deserializationConfig().getPropertyNamingStrategy();

        for (org.springframework.validation.FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            if (namingStrategy != null) {
                field = namingStrategy.nameForField(objectMapper.deserializationConfig(), null, field);
            }
            fieldErrors.add(new FieldError(field, fieldError.getDefaultMessage()));
        }

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(ErrorResponse.from(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getDefaultMessage()
            , fieldErrors));

    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestParameter(MethodArgumentTypeMismatchException e) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(ErrorResponse.from(ErrorCode.VALIDATION_ERROR.name(), "Invalid request parameter",
                List.of(new FieldError(e.getName(), "Invalid value"))));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequestBody(HttpMessageNotReadableException e) {
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(ErrorResponse.from(ErrorCode.VALIDATION_ERROR.name(), "Request body is missing or invalid"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("UNCATEGORIZED_EXCEPTION: ", e);
        return ResponseEntity
            .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatus())
            .body(ErrorResponse.from(ErrorCode.UNCATEGORIZED_EXCEPTION.name(), ErrorCode.UNCATEGORIZED_EXCEPTION.getDefaultMessage()));
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
