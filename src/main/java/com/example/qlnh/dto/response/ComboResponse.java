package com.example.qlnh.dto.response;

import com.example.qlnh.models.entities.Combo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboResponse {
    private Long id;
    private String name;
    private Float price;
    private String description;
    private String status;
    private String imageUrl;

    // 1. Dùng một class riêng để trả kết quả (Thay vì dùng của Request)
    private List<ComboFoodItemResponse> foodItems;

    // 2. Tạo một DTO con tĩnh (static) ngay trong này cho gọn
    @Data
    @AllArgsConstructor
    public static class ComboFoodItemResponse {
        private Long foodId;
        private String foodName; // Frontend cực kỳ cần trường này để hiển thị!
        private Integer quantity;
    }

    // 3. Hàm map chỉ cần truyền duy nhất entity Combo
    public static ComboResponse fromEntity(Combo combo) {
        if (combo == null)
            return null;

        List<ComboFoodItemResponse> items = null;

        // Kiểm tra null để tránh lỗi NullPointerException khi Combo chưa có món
        if (combo.getComboFoods() != null) {
            items = combo.getComboFoods().stream()
                    .map(cf -> new ComboFoodItemResponse(
                            cf.getFood().getId(),
                            cf.getFood().getName(), // Truy xuất trực tiếp tên món ăn
                            cf.getQuantity()))
                    .collect(Collectors.toList());
        }

        return ComboResponse.builder()
                .id(combo.getId())
                .name(combo.getName())
                .price(combo.getPrice())
                .description(combo.getDescription())
                // Ép kiểu Enum an toàn (Nếu bạn đang dùng Enum cho status)
                .status(combo.getStatus() != null ? combo.getStatus().toString() : null)
                .imageUrl(combo.getImageUrl())
                .foodItems(items)
                .build();
    }
}