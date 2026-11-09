package vn.edu.learningai.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.learningai.controller.QuizController;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = QuizController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class QuizExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<QuizError> handleApiException(ApiException exception) {
        return error(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<QuizError> handleInvalidBody(HttpMessageNotReadableException exception) {
        return error(ErrorCode.VALIDATION_ERROR, "Request body is missing or invalid");
    }

    private ResponseEntity<QuizError> error(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus())
            .body(new QuizError(code.name(), message, List.of(), UUID.randomUUID().toString()));
    }

    public record QuizError(String code, String message, List<FieldError> details, String requestId) {}
}
