package com.example.qlnh.services;

import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.Food;
import com.example.qlnh.repositories.ComboFoodRepository;
import com.example.qlnh.repositories.FoodRepository;
import com.example.qlnh.repositories.ReservationFoodRepository;
import com.example.qlnh.services.interfaces.IFoodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FoodService implements IFoodService {

    private final FoodRepository foodRepository;
    private final ComboFoodRepository comboFoodRepository;
    private final ReservationFoodRepository reservationFoodRepository;

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Food> getAllFoods() {
        return foodRepository.findAll();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> getFoods(int page, int itemsPerPage) {
        return foodRepository.findAll(PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalFoods() {
        return foodRepository.count();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Food getFoodById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food", "id", id));
    }

    // TODO: VIET LOGIC
    @Override
    public Food createFood(Food food) {
        return foodRepository.save(food);
    }

    // TODO: VIET LOGIC
    @Override
    public Food updateFood(Food food) {
        Food existingFood = foodRepository.findById(food.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Food", "id", food.getId()));
        existingFood.setName(food.getName());
        existingFood.setDescription(food.getDescription());
        existingFood.setPrice(food.getPrice());
        existingFood.setImageUrl(food.getImageUrl());
        existingFood.setStatus(food.getStatus());
        existingFood.setMealType(food.getMealType());
        return foodRepository.save(existingFood);
    }

    // TODO: VIET LOGIC - xoa food + xoa khoi combo_foods + reservation_foods
    @Override
    public void deleteFood(Long id) {
        throw new UnsupportedOperationException("TODO: Implement deleteFood logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Food> getByMealType(String mealType) {
        return foodRepository.findAllByMealTypeOrderByNameAsc(mealType);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> getFoodsByStatus(int page, int itemsPerPage, String status) {
        return foodRepository.findByStatus(status, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getFoodCountByStatus(String status) {
        return foodRepository.countByStatus(status);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> getFoodsByStatusAndMealType(int page, int itemsPerPage, String status, String mealType) {
        return foodRepository.findByStatusAndMealType(status, mealType, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> getFoodsByMealType(int page, int itemsPerPage, String mealType) {
        return foodRepository.findByMealType(mealType, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> findByKeyword(String keyword, int page, int itemsPerPage) {
        return foodRepository.findByKeyword(keyword, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalFoodsByKeyword(String keyword) {
        return 0;
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage) {
        return foodRepository.findByKeywordAndStatus(keyword, status, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalFoodsByKeywordAndStatus(String keyword, String status) {
        return 0;
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> findByKeywordAndMealType(String keyword, String mealType, int page, int itemsPerPage) {
        return foodRepository.findByKeyword(keyword, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalFoodsByKeywordAndMealType(String keyword, String mealType) {
        return 0;
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Food> findByKeywordAndStatusAndMealType(String keyword, String status, String mealType, int page, int itemsPerPage) {
        return foodRepository.findByKeyword(keyword, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalFoodsByKeywordAndStatusAndMealType(String keyword, String status, String mealType) {
        return 0;
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Food> getFirst6Foods() {
        return foodRepository.findFirst6Foods(PageRequest.of(0, 6)).getContent();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Food> get6FoodsByMealType(String mealType) {
        return foodRepository.find6FoodsByMealType(mealType, PageRequest.of(0, 6)).getContent();
    }
}
