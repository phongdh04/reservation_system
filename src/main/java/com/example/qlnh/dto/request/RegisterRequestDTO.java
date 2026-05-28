package com.example.qlnh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RegisterRequestDTO {
    @NotBlank(message = "Tên không được để trống")
    private String name;
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
    @Size(min = 10, message = "Phone phải có 10 kí tự")
    @NotBlank(message = "Phone không được để trống")
    private String phone;
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 kí tự")
    private String password;
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    // check pass and confirm pass
    public boolean isPasswordMatching() {
        return this.password != null && this.password.equals(this.confirmPassword);
    }

    public void setName(String name) {
        // Nếu name có giá trị, thì trim() nó, nếu null thì giữ nguyên null
        this.name = (name != null) ? name.trim() : null;
    }

    public void setEmail(String email) {
        this.email = (email != null) ? email.trim() : null;
    }

    public void setPassword(String password) {
        this.password = (password != null) ? password.trim() : null;
    }

    public void setPhone(String phone) {
        this.phone = (phone != null) ? phone.trim() : null;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.password = (confirmPassword != null) ? confirmPassword.trim() : null;
    }
}
