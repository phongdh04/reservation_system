package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.FoodResponse;
import com.example.qlnh.models.entities.Food;
import com.example.qlnh.services.interfaces.IFoodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/client/foods")
@RequiredArgsConstructor
public class ClientFoodApiController {

    private final IFoodService foodService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FoodResponse>>> listFoods(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "6") int itemsPerPage,
            @RequestParam(value = "mealType", required = false) String mealType) {
        // TODO: Lay danh sach mon an available cho khach hang
        throw new UnsupportedOperationException("TODO: Implement listFoods logic");
    }
}
