package com.example.qlnh.services.interfaces;

import com.example.qlnh.dto.request.FoodRequest;
import com.example.qlnh.dto.response.FoodResponse;
import com.example.qlnh.models.entities.Food;
import com.example.qlnh.models.entities.User;

import org.springframework.data.domain.Page;

public interface IFoodService {

    // ==========================================
    // 1. SIÊU HÀM TÌM KIẾM & PHÂN TRANG (All-in-One)
    // ==========================================
    // Thay thế cho 17 hàm get, find, count và get6Foods lắt nhắt
    Page<FoodResponse> getAllFoods(String keyword, String status, String mealType, int page, int itemsPerPage);

    // ==========================================
    // 2. NHÓM HÀM THAO TÁC CƠ BẢN (CRUD)
    // ==========================================

    Food getFoodById(Long id);

    Food createFood(FoodRequest request, User currentUser);

    Food updateFood(Long id, FoodRequest req, User currentUser);

    void deleteFood(Long id);
}