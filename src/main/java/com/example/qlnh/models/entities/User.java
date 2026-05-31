package com.example.qlnh.models.entities;

import com.example.qlnh.models.enums.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_role", columnList = "role"),
        @Index(name = "idx_users_phone", columnList = "phone"),
        @Index(name = "idx_users_verification_token", columnList = "verification_token")
})
// 1. Tự động chuyển lệnh DELETE thành UPDATE (Đánh dấu tắt hoạt động)
@SQLDelete(sql = "UPDATE users SET is_active = false WHERE id = ?")
// 2. Tự động lọc: Chỉ kéo lên những User đang hoạt động (is_active = true)
@SQLRestriction("is_active = true")
@Getter
@Setter
@NoArgsConstructor // Bắt buộc phải có cho JPA/Hibernate
@AllArgsConstructor
public class User extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String password;

    // 3. Đảm bảo Enum được lưu dưới dạng chữ (ADMIN, USER...) thay vì số (0, 1...)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token", length = 10)
    private String verificationToken;

    @Column(name = "otp_expiry")
    private java.sql.Timestamp otpExpiry;

    // 4. Cờ Soft Delete (Mặc định tài khoản vừa tạo sẽ luôn tồn tại/hoạt động)
    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;
}