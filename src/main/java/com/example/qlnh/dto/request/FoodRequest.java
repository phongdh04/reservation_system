package com.example.qlnh.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodRequest {
    @NotBlank(message = "Food name is required")
    @Size(max = 255, message = "Food name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private Float price;

    private String imageUrl;

    @NotBlank(message = "Status is required")
    private String status;

    @NotBlank(message = "Meal type is required")
    private String mealType;
}
