package vn.edu.learningai.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ErrorResponse.from(e.getErrorCode().name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {

        List<FieldError> fieldErrors = new ArrayList<>();

        for (org.springframework.validation.FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new FieldError(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .body(ErrorResponse.from(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getDefaultMessage()
            , fieldErrors));

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
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getStatus())
            .body(ErrorResponse.from(
                ErrorCode.UNAUTHORIZED.name(),
                ErrorCode.UNAUTHORIZED.getDefaultMessage()));
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
