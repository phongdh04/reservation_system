package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.ReviewRequest;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.ReviewResponse;
import com.example.qlnh.services.interfaces.IReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/client/reviews")
@RequiredArgsConstructor
public class ClientReviewApiController {

    private final IReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReview(@Valid @RequestBody ReviewRequest request) {
        reviewService.createReview(request);
        log.info("[ClientReview] Khách hàng {} gửi đánh giá thành công", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Gửi đánh giá thành công! Cảm ơn ý kiến của bạn."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> listReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage) {
        Page<ReviewResponse> reviews = reviewService.getAllReviews(null, null, page, itemsPerPage);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", reviews));
    }
}
