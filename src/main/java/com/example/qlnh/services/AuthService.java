package com.example.qlnh.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.qlnh.dto.request.LoginRequestDTO;
import com.example.qlnh.dto.response.LoginResponseDTO;
import com.example.qlnh.exception.BusinessValidationException;
import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.repositories.UserRepository;
import com.example.qlnh.services.interfaces.IAuthService;
// Giả định bạn có class cấu hình JWT, hãy import nó vào (sửa lại đường dẫn cho đúng project của bạn)
import com.example.qlnh.helpers.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponseDTO loginUser(LoginRequestDTO request) {
        // --- 1. authenticate (Xác thực tài khoản & mật khẩu) ---
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // --- 2. loadAndVerifyUser (Kéo User lên và check các điều kiện phụ) ---
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        if (!user.isEmailVerified()) {
            throw new BusinessValidationException("Vui lòng xác nhận email trước khi đăng nhập!");
        }

        // --- 3. Tạo Token ---
        String token = jwtTokenProvider.generateToken(auth);

        // --- 4. buildLoginData (Đóng gói dữ liệu trả về) ---
        return LoginResponseDTO.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}