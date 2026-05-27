package com.example.qlnh.services.interfaces;

public interface IEmailService {
    void sendVerificationEmail(String toEmail, String name, String token);
    void sendOtpEmail(String toEmail, String name, String otp);
}
