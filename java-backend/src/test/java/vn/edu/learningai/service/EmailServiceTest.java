package vn.edu.learningai.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    void passwordResetEmailContainsHtmlAndPlainTextWithoutCallingRealSmtp() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "sender", "learningai@example.test");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173/");

        service.sendPasswordResetEmail("student@example.test", "A".repeat(43));

        ArgumentCaptor<MimeMessage> captured = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captured.capture());
        MimeMessage message = captured.getValue();
        assertThat(message.getSubject()).isEqualTo("Learning AI - Đặt lại mật khẩu");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("student@example.test");
        jakarta.mail.Multipart mixed = (jakarta.mail.Multipart) message.getContent();
        jakarta.mail.Multipart related = (jakarta.mail.Multipart) mixed.getBodyPart(0).getContent();
        jakarta.mail.Multipart alternative = (jakarta.mail.Multipart) related.getBodyPart(0).getContent();
        String link = "http://localhost:5173/reset-password?token=" + "A".repeat(43);
        assertThat(alternative.getBodyPart(0).getContent().toString()).contains(link, "30 phút");
        assertThat(alternative.getBodyPart(1).getContent().toString()).contains(link, "Đặt lại mật khẩu")
            .doesNotContain("{{reset_link}}", "/verify-email");
    }

    @Test
    void passwordResetSmtpFailureBecomesAnApiError() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        doThrow(new MailSendException("Unavailable")).when(sender).send(any(MimeMessage.class));
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "sender", "learningai@example.test");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");

        assertThatThrownBy(() -> service.sendPasswordResetEmail("student@example.test", "token"))
            .isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
    }

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
