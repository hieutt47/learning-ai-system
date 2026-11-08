package vn.edu.learningai.security.custom;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.exception.ErrorResponse;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
        throws IOException, ServletException {
        ErrorCode errorCode = authException instanceof DisabledException
            ? ErrorCode.ACCOUNT_INACTIVE : ErrorCode.UNAUTHENTICATED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = ErrorResponse
            .from(errorCode.name(), errorCode.getDefaultMessage());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
