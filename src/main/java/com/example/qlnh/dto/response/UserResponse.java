package com.example.qlnh.dto.response;

import com.example.qlnh.models.entities.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@Builder
@AllArgsConstructor // Bắt buộc đi kèm với @Builder khi không có @NoArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;

    // Bổ sung thêm 2 trường trạng thái để Frontend vẽ UI (Hiện nút Khóa/Mở tài
    // khoản)
    private boolean isActive;
    private boolean emailVerified;

    // Ép Spring Boot format ngày tháng đẹp đẽ trước khi trả về JSON
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Timestamp createdAt;

    // Hàm Converter chuẩn mực
    public static UserResponse fromEntity(User user) {
        if (user == null)
            return null;

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name()) // Đã fix: Chuyển Enum thành chuỗi String
                .isActive(user.isActive())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}