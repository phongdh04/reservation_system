package com.example.qlnh.services;

import com.example.qlnh.dto.request.ReviewRequest;
import com.example.qlnh.dto.response.ReviewResponse;
import com.example.qlnh.exception.BusinessValidationException;
import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.Review;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.models.enums.UserRole;
import com.example.qlnh.repositories.ReviewRepository;
import com.example.qlnh.repositories.UserRepository;
import com.example.qlnh.services.interfaces.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public boolean createReview(String name, String email, String phone, String content) {
        ReviewRequest request = ReviewRequest.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .rating(5) // Mặc định điểm đánh giá là 5 nếu dùng phương thức gốc
                .content(content)
                .build();
        return createReview(request);
    }

    @Override
    @Transactional
    public boolean createReview(ReviewRequest request) {
        // 1. Kiểm tra lính gác (Guard Clauses)
        if (request == null) {
            log.warn("[Review] Yêu cầu tạo Review rỗng");
            return false;
        }

        // 2. Tìm kiếm hoặc tạo mới khách hàng (User)
        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();
        String name = request.getName().trim();

        User user = userRepository.findByEmailOrPhone(email, phone)
                .orElseGet(() -> {
                    log.info("[Review] Không tìm thấy người dùng với email={} hoặc phone={}. Tiến hành tạo mới CLIENT.", email, phone);
                    User newUser = new User();
                    newUser.setName(name);
                    newUser.setEmail(email);
                    newUser.setPhone(phone);
                    // Mật khẩu tạm thời mã hóa từ số điện thoại
                    newUser.setPassword(passwordEncoder.encode(phone));
                    newUser.setRole(UserRole.CLIENT);
                    newUser.setEmailVerified(true); // Đã liên hệ trực tiếp nên xác thực luôn
                    newUser.setActive(true);
                    return userRepository.save(newUser);
                });

        // Nếu đã có User nhưng tên khác -> tự động cập nhật tên mới nhất của khách
        if (!user.getName().equalsIgnoreCase(name)) {
            user.setName(name);
            user = userRepository.save(user);
        }

        // 3. Tạo và lưu Review
        Review review = new Review();
        review.setCustomer(user);
        review.setRating(request.getRating());
        review.setContent(request.getContent());

        reviewRepository.save(review);
        log.info("[Review] Tạo đánh giá thành công cho khách hàng email={}", email);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(String keyword, Integer rating, int page, int itemsPerPage) {
        // Phân trang an toàn (Giới hạn tối đa 100 record/trang tránh tràn RAM)
        int safePage = Math.max(page - 1, 0);
        int safeItemsPerPage = Math.min(itemsPerPage, 100);

        Pageable pageable = PageRequest.of(safePage, safeItemsPerPage, Sort.by("id").descending());

        // Chuẩn hóa từ khóa tìm kiếm
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Review> reviews = reviewRepository.searchReviews(searchKeyword, rating, pageable);

        return reviews.map(ReviewResponse::fromEntity);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Đánh giá với ID: " + id));

        reviewRepository.delete(review);
        log.info("[Review] Admin xóa đánh giá thành công id={}", id);
    }
}
