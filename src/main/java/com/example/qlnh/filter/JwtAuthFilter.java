package com.example.qlnh.filter;

import com.example.qlnh.helpers.JwtTokenProvider;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.repositories.UserRepository; // ĐỔI IMPORT TỪ Service SANG Repository
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    // 1. Bơm trực tiếp Repository vào đây để tránh lỗi Circular Dependency
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        boolean isAdminApi = requestURI.startsWith(contextPath + "/api/v1/admin/");

        try {
            String jwt = getJwtFromRequest(request);
            if (jwt != null && tokenProvider.validateToken(jwt)) {
                String email = tokenProvider.getUserEmailFromJWT(jwt);

                // 2. Dùng hàm findByEmail của Repo và lấy dữ liệu ra từ Optional
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null) {

                    // 3. TẬN DỤNG ENUM: Không cần switch-case nữa, ghép thẳng chữ ROLE_ với tên
                    // Enum
                    // Ví dụ: user.getRole().name() là "ADMIN" -> grantedRole sẽ là "ROLE_ADMIN"
                    String grantedRole = "ROLE_" + user.getRole().name();

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(grantedRole)));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } else if (isAdminApi) {
                    writeUnauthorized(response, "Unauthorized role");
                    return;
                }
            } else if (isAdminApi) {
                writeUnauthorized(response, "Missing or invalid token");
                return;
            }
        } catch (Exception ex) {
            log.error("Cannot set user authentication", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}