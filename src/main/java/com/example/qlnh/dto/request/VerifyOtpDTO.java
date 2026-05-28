package com.example.qlnh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpDTO {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    // Sử dụng Regex (Biểu thức chính quy) để ép buộc OTP phải là số và có đúng 6 ký
    // tự
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP không hợp lệ (phải bao gồm chính xác 6 chữ số)")
    private String otp;
}