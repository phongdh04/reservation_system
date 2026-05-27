package com.example.qlnh.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComboRequest {
    @NotBlank(message = "Combo name is required")
    @Size(max = 255, message = "Combo name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private Float price;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private String status;
    private String imageUrl;
    private List<ComboFoodDto> foodItems;
}
