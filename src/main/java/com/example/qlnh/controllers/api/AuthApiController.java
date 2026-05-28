package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.RegisterRequestDTO;
import com.example.qlnh.dto.request.VerifyOtpDTO;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.helpers.JwtTokenProvider;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.services.interfaces.IUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final IUserService userService;

    // TODO: VIET LOGIC - Authenticate user bang email/password
    // 1. Kiem tra email da xac thuc chua
    // 2. Generate JWT token
    // 3. Tra ve token + user info
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        throw new UnsupportedOperationException("TODO: Implement login logic");
    }

    // TODO: VIET LOGIC - Dang ky tai khoan moi
    // 1. Validate input
    // 2. Goi userService.registerClient()
    // 3. Gui email OTP
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        userService.registerClient(request);
        log.info("[Auth] New client registered: email={}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Đăng kí thành công!", request.getEmail()));
    }

    // TODO: VIET LOGIC - Xac nhan OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpDTO request) {
        User user = userService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận email thành công! Bạn có thể đăng nhập ngay bây giờ.",
                Map.of("email", user.getEmail(), "name", user.getName())));
    }

    // TODO: VIET LOGIC - Xac nhan email bang token
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        throw new UnsupportedOperationException("TODO: Implement verifyEmail logic");
    }

    // TODO: VIET LOGIC - Lay thong tin user hien tai
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        throw new UnsupportedOperationException("TODO: Implement me logic");
    }
}
