package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.FoodRequest;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.FoodResponse;
import com.example.qlnh.exception.BusinessValidationException;
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

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FoodResponse>>> listFoods(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) String keyword) {

        // Ném thẳng toàn bộ tham số từ URL xuống cho Service xử lý
        Page<FoodResponse> foodPage = foodService.getAllFoods(keyword, status, mealType, page, itemsPerPage);

        // Trả kết quả về cho Client
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách món ăn thành công", foodPage));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FoodResponse>> createFood(@Valid @RequestBody FoodRequest request) {

        // 1. Lấy user đang đăng nhập
        User currentUser = currentUserResolver.resolve();
        if (currentUser == null) {
            throw new BusinessValidationException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập lại!");
        }

        // 2. Ném toàn bộ Request và User xuống Service xử lý
        Food savedFood = foodService.createFood(request, currentUser);

        log.info("[Food] Created id={} name='{}' by={}", savedFood.getId(), savedFood.getName(),
                currentUser.getEmail());

        // 3. Map sang Response và trả về
        return ResponseEntity.ok(ApiResponse.success("Thêm món ăn thành công", FoodResponse.fromEntity(savedFood)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodResponse>> updateFood(
            @PathVariable Long id,
            @Valid @RequestBody FoodRequest request) {

        // 1. Lấy thông tin User đang thao tác
        User currentUser = currentUserResolver.resolve();
        if (currentUser == null) {
            throw new BusinessValidationException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập lại!");
        }

        // 2. Giao việc cho Service (Tìm, Validate, Cập nhật, Lưu DB)
        Food updatedFood = foodService.updateFood(id, request, currentUser);

        log.info("[Food] Updated id={} by={}", id, currentUser.getEmail());

        // 3. Trả về kết quả
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật món ăn thành công", FoodResponse.fromEntity(updatedFood)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFood(@PathVariable Long id) {

        // Giao việc xóa cho Service
        foodService.deleteFood(id);

        log.info("[Food] Deleted id={}", id);
        return ResponseEntity.ok(ApiResponse.success("Xóa món ăn thành công"));
    }
}
