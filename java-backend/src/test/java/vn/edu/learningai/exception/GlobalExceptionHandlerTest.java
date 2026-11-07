package vn.edu.learningai.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void malformedJsonReturnsValidationErrorInsteadOfInternalServerError() {
        var response = handler.handleUnreadableRequest(new HttpMessageNotReadableException("Invalid JSON", null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().getCode());
    }

    @Test
    void multipartLimitReturnsPayloadTooLarge() {
        var response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10L * 1024 * 1024));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(ErrorCode.FILE_TOO_LARGE.name(), response.getBody().getCode());
    }
}
