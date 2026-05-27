package com.example.qlnh.dto.response;

import com.example.qlnh.models.entities.Food;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResponse {
    private Long id;
    private String name;
    private String description;
    private Float price;
    private String imageUrl;
    private String status;
    private String mealType;
    private String createdByName;
    private LocalDateTime createdAt;

    public static FoodResponse fromEntity(Food food) {
        if (food == null) return null;
        return FoodResponse.builder()
                .id(food.getId())
                .name(food.getName())
                .description(food.getDescription())
                .price(food.getPrice())
                .imageUrl(food.getImageUrl())
                .status(food.getStatus())
                .mealType(food.getMealType())
                .createdByName(food.getCreatedBy() != null ? food.getCreatedBy().getName() : null)
                .createdAt(food.getCreatedAt() != null ? food.getCreatedAt().toLocalDateTime() : null)
                .build();
    }
}
