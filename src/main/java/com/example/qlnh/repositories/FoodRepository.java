package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Food;
import com.example.qlnh.models.enums.FoodStatus;
import com.example.qlnh.models.enums.MealType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {

    // ==========================================
    // 1. NHÓM KIỂM TRA (Dùng cho Create/Update)
    // ==========================================
    boolean existsByName(String name);

    // ==========================================
    // 2. SIÊU HÀM TÌM KIẾM & LỌC (All-in-One)
    // ==========================================
    // Giải quyết phân trang + Lọc + Chống N+1 Query cực kỳ thanh lịch
    @EntityGraph(attributePaths = { "createdBy" })
    @Query("SELECT f FROM Food f WHERE " +
            "(:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR f.status = :status) " +
            "AND (:mealType IS NULL OR f.mealType = :mealType)")
    Page<Food> searchFoods(@Param("keyword") String keyword,
            @Param("status") FoodStatus status,
            @Param("mealType") MealType mealType,
            Pageable pageable);

    // ==========================================
    // 3. NHÓM TRUY VẤN NGHIỆP VỤ HÓA ĐƠN (ORDER/BILL)
    // ==========================================

    // Tự động sinh SQL: Tìm danh sách món theo tên và kiểm tra xem có đang bán
    // không
    // Thay thế cho hàm findByNameInAndStatusAvailable cũ
    List<Food> findByNameInAndStatus(List<String> names, FoodStatus status);

    // Phục vụ Frontend lấy nhanh danh sách Menu theo từng loại (Khai vị, Món
    // chính...) không cần phân trang
    @EntityGraph(attributePaths = { "createdBy" })
    List<Food> findByStatusAndMealTypeOrderByNameAsc(FoodStatus status, MealType mealType);

    // (Tùy chọn) Xem các món ăn do 1 nhân viên cụ thể tạo ra
    @EntityGraph(attributePaths = { "createdBy" })
    List<Food> findByCreatedById(Long userId);
}