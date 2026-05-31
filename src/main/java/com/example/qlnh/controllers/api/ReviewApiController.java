package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.ReviewResponse;
import com.example.qlnh.services.interfaces.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final IReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> listReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String keyword) {

        Page<ReviewResponse> reviews = reviewService.getAllReviews(keyword, rating, page, itemsPerPage);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", reviews));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        log.info("[AdminReview] Admin xóa đánh giá id={} thành công", id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công"));
    }
}
