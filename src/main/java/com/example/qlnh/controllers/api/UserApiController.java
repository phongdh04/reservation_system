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
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "keyword", required = false) String keyword) {
        // TODO: GOI userService de lay danh sach user
        throw new UnsupportedOperationException("TODO: Implement listUsers logic");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser(@Valid @RequestBody CreateUserRequest req) {
        // TODO: Validate + tao user
        throw new UnsupportedOperationException("TODO: Implement createUser logic");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        // TODO: Lay user cu + update
        throw new UnsupportedOperationException("TODO: Implement updateUser logic");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        // TODO: Xoa user (khong cho xoa admin dang login)
        throw new UnsupportedOperationException("TODO: Implement deleteUser logic");
    }
}
