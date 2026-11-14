package vn.edu.learningai.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @Test
    void smtpFailureBecomesAnApiError() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        doThrow(new MailSendException("Unavailable")).when(sender).send(any(MimeMessage.class));
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "sender", "learningai@example.test");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");
        assertThatThrownBy(() -> service.sendVerificationEmail("student@example.test", "token"))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
    }
}
