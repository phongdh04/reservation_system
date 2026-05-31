package com.example.qlnh.services.interfaces;

import com.example.qlnh.dto.request.VerifyOtpDTO;
import com.example.qlnh.dto.response.UserResponse;
import com.example.qlnh.dto.request.CreateUserRequest;
import com.example.qlnh.dto.request.RegisterRequestDTO;
import com.example.qlnh.dto.request.UpdateUserRequest;
import com.example.qlnh.models.entities.User;
import org.springframework.data.domain.Page;

public interface IUserService {

    // ==========================================
    // TÍNH NĂNG QUẢN TRỊ (ADMIN)
    // ==========================================

    // Siêu hàm: Bao gồm Lấy danh sách, Phân trang, Tìm kiếm, và Lọc theo Role
    Page<UserResponse> getAllUsers(String keyword, String role, int page, int itemsPerPage);

    User getUserById(Long id);

    User createUser(CreateUserRequest request);

    User updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id, Long currentAdminId);

    // ==========================================
    // TÍNH NĂNG KHÁCH HÀNG (CLIENT)
    // ==========================================

    User registerClient(RegisterRequestDTO request);

    User verifyOtp(VerifyOtpDTO request);
}