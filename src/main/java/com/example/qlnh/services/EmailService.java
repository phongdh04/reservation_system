package com.example.qlnh.services;

import com.example.qlnh.services.interfaces.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;

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
    @Async
    @Override
    public void sendOtpEmail(String toEmail, String name, String otp) {
        throw new UnsupportedOperationException("TODO: Implement sendOtpEmail logic");
    }
}
