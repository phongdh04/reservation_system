package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.ComboFood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboFoodRepository extends JpaRepository<ComboFood, Long> {
    void deleteByFoodId(Long foodId);
    void deleteByComboId(Long comboId);
    List<ComboFood> findByComboId(Long comboId);
}
