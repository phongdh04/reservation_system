package com.example.qlnh.services.interfaces;

import com.example.qlnh.dto.request.ReviewRequest;
import com.example.qlnh.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;

public interface IReviewService {
    // 1. Tạo đánh giá (phương thức gốc)
    boolean createReview(String name, String email, String phone, String content);

    // 2. Tạo đánh giá mới sử dụng DTO
    boolean createReview(ReviewRequest request);

    // 3. Phân trang lấy danh sách đánh giá
    Page<ReviewResponse> getAllReviews(String keyword, Integer rating, int page, int itemsPerPage);

    // 4. Xóa đánh giá (chỉ dành cho Admin)
    void deleteReview(Long id);
}
