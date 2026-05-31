package com.example.qlnh.dto.response;

import com.example.qlnh.models.entities.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Integer rating;
    private String content;
    private Timestamp createdAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }
        return ReviewResponse.builder()
                .id(review.getId())
                .customerId(review.getCustomer() != null ? review.getCustomer().getId() : null)
                .customerName(review.getCustomer() != null ? review.getCustomer().getName() : null)
                .customerEmail(review.getCustomer() != null ? review.getCustomer().getEmail() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
