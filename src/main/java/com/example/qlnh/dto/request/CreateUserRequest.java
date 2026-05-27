package com.example.qlnh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    private String phone;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Password is required")
    private String password;

    private String confirmPassword;
}
