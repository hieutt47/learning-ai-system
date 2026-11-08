package vn.edu.learningai.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.edu.learningai.config.RequestIdFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {
    /**
     * The handler runs with this mapper so the two malformed-body tests do not
     * depend on Spring's message converters; field names are not under test
     * here, so an empty builder is enough.
     */
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
        // The base class declares the body as Object; the advice always puts an ErrorResponse there.
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.FILE_TOO_LARGE.name(), body.getCode());
        assertNotNull(body.getRequestId());
    }

    /** Error bodies always carry a request id, so the handler reads it from the request. */
    private MockHttpServletRequest requestWithId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "test-request-id");
        return request;
    }
}
