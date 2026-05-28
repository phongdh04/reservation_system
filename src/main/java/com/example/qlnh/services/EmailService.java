package com.example.qlnh.services;

import com.example.qlnh.services.interfaces.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;
    // Tiêm (Inject) công cụ xử lý HTML của Thymeleaf vào
    private final SpringTemplateEngine templateEngine;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    // TODO: VIET LOGIC - gui email xac nhan tai khoan voi link
    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String name, String token) {
        throw new UnsupportedOperationException("TODO: Implement sendVerificationEmail logic");
    }

    // TODO: VIET LOGIC - gui email OTP
    @Async("emailExecutor")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Override
    public void sendOtpEmail(String toEmail, String name, String otp) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("otp", otp);

            // 2. Thymeleaf: Lấy file "otp-email.html" và nhét dữ liệu vào
            // Trả về một chuỗi HTML đã hoàn thiện
            String htmlBody = templateEngine.process("otp-email", context);

            // 3. Gửi email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Mã xác nhận OTP - Nhà Hàng");

            // Set nội dung là chuỗi HTML vừa tạo, tham số 'true' báo cho Gmail biết đây là
            // code HTML
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Gửi OTP thành công tới {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email tới {}: {}", toEmail, e.getMessage());
            // Throw để @Retryable biết là có lỗi và thử gửi lại
            throw new RuntimeException("Lỗi gửi email", e);
        }
    }
}
