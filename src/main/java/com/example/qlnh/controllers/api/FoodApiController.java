package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.FoodRequest;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.FoodResponse;
import com.example.qlnh.helpers.CurrentUserResolver;
import com.example.qlnh.models.entities.Food;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.services.interfaces.IFoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/foods")
@RequiredArgsConstructor
public class FoodApiController {

    private final IFoodService foodService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<FoodResponse>>> listFoods(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "mealType", required = false) String mealType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        // TODO: Lay danh sach mon an voi filter
        throw new UnsupportedOperationException("TODO: Implement listFoods logic");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FoodResponse>> createFood(@Valid @RequestBody FoodRequest req) {
        // TODO: Tao mon an moi (set createdBy = currentUser)
        throw new UnsupportedOperationException("TODO: Implement createFood logic");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodResponse>> updateFood(@PathVariable Long id, @Valid @RequestBody FoodRequest req) {
        // TODO: Cap nhat mon an
        throw new UnsupportedOperationException("TODO: Implement updateFood logic");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFood(@PathVariable Long id) {
        // TODO: Xoa mon an
        throw new UnsupportedOperationException("TODO: Implement deleteFood logic");
    }
}
