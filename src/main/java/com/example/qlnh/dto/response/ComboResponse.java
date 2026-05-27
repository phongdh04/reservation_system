package com.example.qlnh.dto.response;

import com.example.qlnh.dto.request.ComboFoodDto;
import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.entities.ComboFood;
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
    private List<ComboFoodDto> foodItems;

    public static ComboResponse fromEntity(Combo combo, List<ComboFood> comboFoods) {
        if (combo == null) return null;
        List<ComboFoodDto> foodItems = comboFoods.stream()
                .map(cf -> new ComboFoodDto(cf.getFood().getId(), cf.getQuantity()))
                .collect(Collectors.toList());
        return ComboResponse.builder()
                .id(combo.getId())
                .name(combo.getName())
                .price(combo.getPrice())
                .description(combo.getDescription())
                .status(combo.getStatus())
                .imageUrl(combo.getImageUrl())
                .foodItems(foodItems)
                .build();
    }
}
