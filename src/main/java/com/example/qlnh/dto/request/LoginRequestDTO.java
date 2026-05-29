package com.example.qlnh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    // Lưu ý: Lúc Login KHÔNG NÊN validate độ dài hay format của mật khẩu (như
    // @Pattern hay @Size)
    // Chỉ cần check NotBlank là đủ. Nếu sai mật khẩu, Service sẽ tự văng lỗi.
    private String password;

}