package com.example.qlnh.services.interfaces;

import com.example.qlnh.models.entities.Food;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IFoodService {

    List<Food> getAllFoods();
    Page<Food> getFoods(int page, int itemsPerPage);
    long getTotalFoods();
    Food getFoodById(Long id);
    Food createFood(Food food);
    Food updateFood(Food food);
    void deleteFood(Long id);
    List<Food> getByMealType(String mealType);
    Page<Food> getFoodsByStatus(int page, int itemsPerPage, String status);
    long getFoodCountByStatus(String status);
    Page<Food> getFoodsByStatusAndMealType(int page, int itemsPerPage, String status, String mealType);
    Page<Food> getFoodsByMealType(int page, int itemsPerPage, String mealType);
    Page<Food> findByKeyword(String keyword, int page, int itemsPerPage);
    long getTotalFoodsByKeyword(String keyword);
    Page<Food> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage);
    long getTotalFoodsByKeywordAndStatus(String keyword, String status);
    Page<Food> findByKeywordAndMealType(String keyword, String mealType, int page, int itemsPerPage);
    long getTotalFoodsByKeywordAndMealType(String keyword, String mealType);
    Page<Food> findByKeywordAndStatusAndMealType(String keyword, String status, String mealType, int page, int itemsPerPage);
    long getTotalFoodsByKeywordAndStatusAndMealType(String keyword, String status, String mealType);
    List<Food> getFirst6Foods();
    List<Food> get6FoodsByMealType(String mealType);
}
