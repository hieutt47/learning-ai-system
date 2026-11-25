package vn.edu.learningai.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.edu.learningai.config.RequestIdFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler =
        new GlobalExceptionHandler(tools.jackson.databind.json.JsonMapper.builder().build());
    @Test
    void malformedJsonReturnsValidationErrorInsteadOfInternalServerError() {
        var response = handler.handleHttpMessageNotReadable(
            new HttpMessageNotReadableException("Invalid JSON", null),
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            new ServletWebRequest(requestWithId()));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), body.getCode());
        assertNotNull(body.getRequestId());
    }
    @Test
    void multipartLimitReturnsPayloadTooLarge() {
        var response = handler.handleMaxUploadSizeExceededException(
            new MaxUploadSizeExceededException(10L * 1024 * 1024),
            new HttpHeaders(),
            HttpStatus.PAYLOAD_TOO_LARGE,
            new ServletWebRequest(requestWithId()));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.FILE_TOO_LARGE.name(), body.getCode());
        assertNotNull(body.getRequestId());
    }
    @Test
    void validationErrorsIncludeSpecificFieldMessagesInResponseMessage() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "courseWriteRequest");
        bindingResult.addError(new FieldError("courseWriteRequest", "title", "Tên khóa học không được để trống"));
        bindingResult.addError(new FieldError("courseWriteRequest", "description", "Mô tả khóa học không được để trống"));
        var method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class);
        var exception = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleMethodArgumentNotValid(
            exception,
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            new ServletWebRequest(requestWithId()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), body.getCode());
        assertTrue(body.getMessage().contains("Tên khóa học không được để trống"));
        assertTrue(body.getMessage().contains("Mô tả khóa học không được để trống"));
        assertEquals(2, body.getDetails().size());
    }
    @SuppressWarnings("unused")
    private void dummyMethod(String value) {}
    private MockHttpServletRequest requestWithId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "test-request-id");
        return request;
    }
}
