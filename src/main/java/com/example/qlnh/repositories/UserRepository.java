package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.User;
import com.example.qlnh.models.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

        // ==========================================
        // 1. SIÊU HÀM TÌM KIẾM (Cho Admin)
        // ==========================================
        @Query("SELECT u FROM User u WHERE " +
                        "(:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " + // Đã bổ sung tìm theo SĐT
                        "AND (:role IS NULL OR u.role = :role)")
        Page<User> searchUsers(@Param("keyword") String keyword,
                        @Param("role") UserRole role,
                        Pageable pageable);

        // ==========================================
        // 2. NHÓM HÀM XÁC THỰC & KIỂM TRA (Auth & Validate)
        // ==========================================

        // Bắt buộc dùng Optional để tránh NullPointerException khi Login/Verify
        Optional<User> findByEmail(String email);

        // Dùng cho hàm Create và Update User để check trùng Email
        boolean existsByEmail(String email);

        // Tìm kiếm khách hàng theo email hoặc số điện thoại
        Optional<User> findByEmailOrPhone(String email, String phone);
}