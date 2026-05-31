package com.example.qlnh.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.example.qlnh.exception.BusinessValidationException;
import com.example.qlnh.exception.DuplicateResourceException;
import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.models.enums.UserRole;
import com.example.qlnh.repositories.UserRepository;
import com.example.qlnh.services.interfaces.IEmailService;
import com.example.qlnh.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.qlnh.dto.request.CreateUserRequest;
import com.example.qlnh.dto.request.RegisterRequestDTO;
import com.example.qlnh.dto.request.UpdateUserRequest;
import com.example.qlnh.dto.request.VerifyOtpDTO;
import com.example.qlnh.dto.response.UserResponse;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String keyword, String role, int page, int itemsPerPage) {

        // --- 1. THIẾT LẬP CHỐT CHẶN AN TOÀN ---
        // Ngăn chặn Frontend truyền số trang âm
        int safePage = Math.max(page - 1, 0);

        // Ngăn chặn thảm họa tràn RAM: Dù Client truyền 1 triệu, ta chỉ cho tối đa 100
        // record/trang
        int safeItemsPerPage = Math.min(itemsPerPage, 100);

        // Tạo đối tượng Pageable và sắp xếp User mới tạo lên đầu
        Pageable pageable = PageRequest.of(safePage, safeItemsPerPage, Sort.by("createdAt").descending());

        // --- 2. XỬ LÝ LỌC ENUM (An toàn) ---
        UserRole userRole = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                userRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException("Role không hợp lệ để lọc!");
            }
        }

        // --- 3. GỌI DATABASE ---
        // Hàm này bạn sẽ định nghĩa trong UserRepository
        Page<User> users = userRepository.searchUsers(keyword, userRole, pageable);

        // --- 4. MAP SANG DTO TRẢ VỀ ---
        return users.map(UserResponse::fromEntity); // Nếu bạn có viết hàm static fromEntity trong DTO
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        // 1. Kiểm tra nghiệp vụ (Validate)
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessValidationException("Mật khẩu xác nhận không khớp!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessValidationException("Email này đã được sử dụng!");
        }
        // 2. Chuyển đổi DTO -> Entity (Tương đương hàm buildNewUser của bạn)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        // --- Xử lý Enum Role an toàn ---
        try {
            // Ép chuỗi thành viết hoa (đề phòng Frontend gửi "admin" chữ thường)
            // rồi chuyển sang Enum
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            // Nếu Frontend gửi sai tên Role, chặn lại và báo lỗi ngay!
            throw new BusinessValidationException("Quyền (Role) không hợp lệ! Chỉ chấp nhận: ADMIN, USER...");
        }

        // 3. Logic đặc thù khi Admin tạo tài khoản
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(true); // Admin tạo thì tự động active
        return userRepository.save(user);
    }

    @Override
    @Transactional // Cực kỳ quan trọng khi Update
    public User updateUser(Long id, UpdateUserRequest request) {

        // 1. Tìm User (Nếu không có thì văng lỗi 404 ngay)
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        // 2. Validate và Cập nhật Email (Logic hóc búa nhất)
        // Chỉ check trùng lặp nếu người dùng thực sự muốn đổi sang một email khác
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessValidationException("Email này đã được sử dụng bởi một tài khoản khác!");
            }
            user.setEmail(request.getEmail());
        }

        // 3. Cập nhật các trường thông tin cơ bản
        if (request.getName() != null)
            user.setName(request.getName());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());

        // 4. Xử lý Enum Role an toàn (Giống y hệt lúc Create)
        if (request.getRole() != null) {
            try {
                user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException("Quyền (Role) không hợp lệ! Chỉ chấp nhận: ADMIN, USER...");
            }
        }
        // 5. Lưu xuống DB
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long targetUserId, Long currentAdminId) {

        // 1. Lính gác số 1: Chống "Tự sát" (Admin không được tự xóa chính mình)
        if (targetUserId.equals(currentAdminId)) {
            throw new BusinessValidationException(
                    "Hành động bị từ chối: Bạn không thể tự xóa tài khoản của chính mình!");
        }

        // 2. Lính gác số 2: Kiểm tra User cần xóa có tồn tại không
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + targetUserId));

        // 3. Thực hiện xóa
        userRepository.delete(targetUser);
    }

    @Override
    @Transactional
    public User registerClient(RegisterRequestDTO request) {
        String email = request.getEmail();
        String name = request.getName();
        String password = request.getPassword();
        String phone = request.getPhone();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email đã tồn tại" + email);
        }
        // Sinh OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new Random().nextInt(999999));
        java.sql.Timestamp expiry = java.sql.Timestamp.valueOf(
                java.time.LocalDateTime.now().plusMinutes(15));

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.CLIENT); // Hoặc UserRole.USER (Tùy thuộc vào tên bạn đặt trong file Enum)
        user.setEmailVerified(false);
        user.setVerificationToken(otp);
        user.setOtpExpiry(expiry);

        User saved = userRepository.save(user);
        log.info("Registered new client: {}", email);

        // Gửi OTP qua email (async)
        emailService.sendOtpEmail(email, name, otp);

        return saved;
    }

    @Override
    @Transactional
    public User verifyOtp(VerifyOtpDTO request) {
        String email = request.getEmail();
        String otp = request.getOtp();

        // 1. SỬA Ở ĐÂY: Vừa tìm kiếm, vừa ném lỗi nếu không thấy (thay thế luôn cho
        // khối if == null)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email này"));

        // Các đoạn check phía sau giữ nguyên...
        if (user.isEmailVerified()) {
            throw new BusinessValidationException("Tài khoản đã được xác nhận rồi.");
        }

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(otp)) {
            throw new BusinessValidationException("Mã OTP không đúng. Vui lòng kiểm tra lại!");
        }

        if (user.getOtpExpiry() == null
                || user.getOtpExpiry().before(new java.sql.Timestamp(System.currentTimeMillis()))) {
            throw new BusinessValidationException("Mã OTP đã hết hạn. Vui lòng đăng ký lại.");
        }

        // Cập nhật trạng thái
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setOtpExpiry(null);

        return userRepository.save(user);
    }

}
