package com.example.qlnh.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_role",                columnList = "role"),
        @Index(name = "idx_users_phone",               columnList = "phone"),
        @Index(name = "idx_users_verification_token",  columnList = "verification_token")
    }
)
@Getter
@Setter
@RequiredArgsConstructor
public class User extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token", length = 10)
    private String verificationToken;

    @Column(name = "otp_expiry")
    private java.sql.Timestamp otpExpiry;
}
