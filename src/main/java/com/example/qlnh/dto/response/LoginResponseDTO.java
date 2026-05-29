package com.example.qlnh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder // Giúp tạo đối tượng nhanh gọn bằng Builder Pattern ở tầng Service
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer"; // Chuẩn hóa loại token cho Frontend dễ cấu hình Axios

    private String email;

    private String name;

    private String role; // Trả về chuỗi (ROLE_USER, ROLE_ADMIN...) để React phân quyền router
}