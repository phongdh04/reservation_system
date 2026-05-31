package com.example.qlnh.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.qlnh.dto.request.FoodRequest;
import com.example.qlnh.dto.response.FoodResponse;
import com.example.qlnh.exception.BusinessValidationException;
import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.Food;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.models.enums.FoodStatus;
import com.example.qlnh.models.enums.MealType;
import com.example.qlnh.repositories.FoodRepository;
import com.example.qlnh.services.interfaces.IFoodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FoodService implements IFoodService {

    private final FoodRepository foodRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FoodResponse> getAllFoods(String keyword, String status, String mealType, int page, int itemsPerPage) {

        // 1. Xử lý phân trang an toàn (Tránh user cố tình truyền số âm hoặc lấy quá
        // nhiều data)
        int safePage = Math.max(page - 1, 0);
        int safeItemsPerPage = Math.min(itemsPerPage, 100);

        // Cấu hình phân trang (Nên sắp xếp theo ID giảm dần để món mới thêm hiện lên
        // đầu)
        Pageable pageable = PageRequest.of(safePage, safeItemsPerPage, Sort.by("id").descending());

        // 2. Ép kiểu Status sang Enum một cách an toàn
        FoodStatus foodStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                foodStatus = FoodStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException(
                        "Trạng thái lọc không hợp lệ! (Chỉ dùng: AVAILABLE, UNAVAILABLE)");
            }
        }

        // 3. Ép kiểu MealType sang Enum một cách an toàn
        MealType foodMealType = null;
        if (mealType != null && !mealType.trim().isEmpty() && !mealType.equalsIgnoreCase("all")) {
            try {
                foodMealType = MealType.valueOf(mealType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException(
                        "Loại món ăn không hợp lệ! (Chỉ dùng: BREAKFAST, LUNCH, DINNER, DESSERT)");
            }
        }

        // 4. Gọi DB để lấy dữ liệu (Ném tất cả thông số đã được làm sạch xuống
        // Repository)
        Page<Food> foods = foodRepository.searchFoods(keyword, foodStatus, foodMealType, pageable);

        // 5. Chuyển đổi từ Entity sang DTO và trả về cho Controller
        return foods.map(FoodResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Food getFoodById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food", "id", id));
    }

    @Override
    @Transactional
    public Food createFood(FoodRequest request, User currentUser) {

        // 1. Lính gác: Kiểm tra trùng tên món ăn (để Menu không bị lặp)
        if (foodRepository.existsByName(request.getName())) {
            throw new BusinessValidationException("Tên món ăn '" + request.getName() + "' đã tồn tại trong thực đơn!");
        }

        // 2. Nhào nặn dữ liệu cơ bản (Mapping DTO -> Entity)
        Food food = new Food();
        food.setName(request.getName().trim());
        food.setDescription(request.getDescription());
        food.setPrice(request.getPrice());
        food.setImageUrl(request.getImageUrl());

        // 3. Xử lý Enum an toàn cho Trạng thái (Status)
        // Giả sử bạn có Enum FoodStatus(AVAILABLE, OUT_OF_STOCK...)
        try {
            food.setStatus(FoodStatus.valueOf(request.getStatus().trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException(
                    "Trạng thái món ăn không hợp lệ! Vui lòng kiểm tra lại (VD: AVAILABLE, OUT_OF_STOCK...).");
        }

        // 4. Xử lý Enum an toàn cho Phân loại món (Meal Type)
        // Giả sử bạn có Enum MealType(APPETIZER, MAIN_COURSE, DESSERT, DRINK...)
        try {
            food.setMealType(MealType.valueOf(request.getMealType().trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException(
                    "Loại món ăn không hợp lệ! Vui lòng kiểm tra lại (VD: MAIN_COURSE, DRINK...).");
        }

        // 5. Gắn thông tin Audit (Ai là người tạo món này)
        food.setCreatedBy(currentUser);

        // 6. Lưu xuống Database
        return foodRepository.save(food);
    }

    @Override
    @Transactional
    public Food updateFood(Long id, FoodRequest request, User currentUser) {

        // 1. Tìm món ăn (Ném lỗi 404 cho GlobalExceptionHandler nếu không thấy)
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn với ID: " + id));

        // 2. Validate và Cập nhật Tên món
        // Chỉ check trùng lặp nếu người dùng thực sự muốn đổi sang một cái tên khác
        if (request.getName() != null && !request.getName().trim().equals(food.getName())) {
            if (foodRepository.existsByName(request.getName().trim())) {
                throw new BusinessValidationException(
                        "Tên món ăn '" + request.getName() + "' đã tồn tại trong thực đơn!");
            }
            food.setName(request.getName().trim());
        }

        // 3. Cập nhật các trường dữ liệu cơ bản
        if (request.getDescription() != null)
            food.setDescription(request.getDescription());
        if (request.getPrice() != null)
            food.setPrice(request.getPrice());
        if (request.getImageUrl() != null)
            food.setImageUrl(request.getImageUrl());

        // 4. Cập nhật Status (An toàn với Enum)
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                food.setStatus(FoodStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException("Trạng thái không hợp lệ! (Chỉ dùng: AVAILABLE, UNAVAILABLE)");
            }
        }

        // 5. Cập nhật MealType (An toàn với Enum)
        if (request.getMealType() != null && !request.getMealType().trim().isEmpty()) {
            try {
                food.setMealType(MealType.valueOf(request.getMealType().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessValidationException(
                        "Loại món ăn không hợp lệ! (Chỉ dùng: BREAKFAST, LUNCH, DINNER, DESSERT)");
            }
        }

        // (Tùy chọn) Ghi nhận lại người vừa chỉnh sửa món ăn này nếu Entity của bạn có
        // trường updatedBy
        // food.setUpdatedBy(currentUser);

        // 6. Lưu xuống DB
        return foodRepository.save(food);
    }

    @Transactional
    public void deleteFood(Long id) {

        // 1. Tìm món ăn (Ném lỗi 404 nếu không tồn tại)
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn với ID: " + id));

        // 2. Thực hiện xóa
        // (Sẽ tự động biến thành UPDATE Xóa mềm nếu đã cấu hình @SQLDelete ở Entity)
        foodRepository.delete(food);
    }
}
