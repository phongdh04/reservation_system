package com.example.qlnh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComboFoodDto {
    private Long foodId;
    private Integer quantity;
}
