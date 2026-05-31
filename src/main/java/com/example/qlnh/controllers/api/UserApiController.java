package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.CreateUserRequest;
import com.example.qlnh.dto.request.UpdateUserRequest;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.UserResponse;
import com.example.qlnh.helpers.CurrentUserResolver;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.services.interfaces.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserApiController {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {

        // Ném toàn bộ tham số xuống Service xử lý
        Page<UserResponse> userPage = userService.getAllUsers(keyword, role, page, itemsPerPage);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", userPage));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser(@Valid @RequestBody CreateUserRequest request) {
        userService.createUser(request);

        log.info("[User] Admin created user email={} role={}", request.getEmail(), request.getRole());
        return ResponseEntity.ok(ApiResponse.success("Tạo người dùng thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        // Ném ID và Request xuống cho Service lo toàn bộ
        userService.updateUser(id, request);

        log.info("[User] Admin updated user id={}", id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {

        // Lấy thông tin Admin đang gọi API này
        User currentUser = currentUserResolver.resolve();

        // Giao phó toàn bộ logic cho Service (Truyền ID cần xóa và ID của Admin vào)
        userService.deleteUser(id, currentUser.getId());

        log.info("[User] Admin (id={}) deleted user (id={})", currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công"));
    }
}
