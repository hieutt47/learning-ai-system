package vn.edu.learningai.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public void sendVerificationEmail(String email, String token) {
        String link = frontendBaseUrl.replaceAll("/+$", "") + "/verify-email?token=" + token;
        try {
            String template = new ClassPathResource("templates/verification-email.html")
                .getContentAsString(StandardCharsets.UTF_8);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender, "Learning AI");
            helper.setTo(email);
            helper.setSubject("Learning AI - Xác minh địa chỉ email");
            helper.setText("Xác minh email Learning AI: " + link + "\nLink có hiệu lực trong 24 giờ.",
                template.replace("{{verification_link}}", HtmlUtils.htmlEscape(link)));
            mailSender.send(message);
        } catch (MessagingException | MailException | IOException e) {
            throw new ApiException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    public void sendPasswordResetEmail(String email, String token) {
        String link = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        try {
            String template = new ClassPathResource("templates/password-reset-email.html")
                .getContentAsString(StandardCharsets.UTF_8);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender, "Learning AI");
            helper.setTo(email);
            helper.setSubject("Learning AI - Đặt lại mật khẩu");
            helper.setText("Đặt lại mật khẩu Learning AI: " + link + "\nLink có hiệu lực trong 30 phút."
                    + "\nNếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.",
                template.replace("{{reset_link}}", HtmlUtils.htmlEscape(link)));
            mailSender.send(message);
        } catch (MessagingException | MailException | IOException e) {
            throw new ApiException(ErrorCode.EMAIL_SEND_FAILED, "Could not send password reset email");
        }
    }
}
