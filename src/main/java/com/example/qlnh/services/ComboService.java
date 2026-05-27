package com.example.qlnh.services;

import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.entities.ComboFood;
import com.example.qlnh.models.entities.Food;
import com.example.qlnh.repositories.ComboFoodRepository;
import com.example.qlnh.repositories.ComboRepository;
import com.example.qlnh.repositories.FoodRepository;
import com.example.qlnh.repositories.ReservationComboRepository;
import com.example.qlnh.services.interfaces.IComboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService implements IComboService {

    private final ComboRepository comboRepository;
    private final ReservationComboRepository reservationComboRepository;
    private final ComboFoodRepository comboFoodRepository;
    private final FoodRepository foodRepository;

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Combo> getAllCombos() {
        return comboRepository.findAll();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Combo> getCombosByPage(int page, int itemsPerPage) {
        return comboRepository.findAll(PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalCombos() {
        return comboRepository.count();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Combo getComboById(Long id) {
        return comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo", "id", id));
    }

    // TODO: VIET LOGIC
    @Override
    public Combo createCombo(Combo combo) {
        return comboRepository.save(combo);
    }

    // TODO: VIET LOGIC
    @Override
    public Combo updateCombo(Combo combo) {
        Combo existingCombo = comboRepository.findById(combo.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Combo", "id", combo.getId()));
        existingCombo.setName(combo.getName());
        existingCombo.setPrice(combo.getPrice());
        existingCombo.setDescription(combo.getDescription());
        existingCombo.setStatus(combo.getStatus());
        existingCombo.setImageUrl(combo.getImageUrl());
        return comboRepository.save(existingCombo);
    }

    // TODO: VIET LOGIC - tao combo + luu combo_foods
    @Override
    @Transactional
    public Combo createComboWithFoods(com.example.qlnh.dto.request.ComboRequest request) {
        throw new UnsupportedOperationException("TODO: Implement createComboWithFoods logic");
    }

    // TODO: VIET LOGIC - update combo + xoa/tao lai combo_foods
    @Override
    @Transactional
    public Combo updateComboWithFoods(Long id, com.example.qlnh.dto.request.ComboRequest request) {
        throw new UnsupportedOperationException("TODO: Implement updateComboWithFoods logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<ComboFood> getComboFoods(Long comboId) {
        return comboFoodRepository.findByComboId(comboId);
    }

    // TODO: VIET LOGIC - kiem tra reservation_combos truoc khi xoa
    @Override
    public boolean deleteCombo(Long id) {
        throw new UnsupportedOperationException("TODO: Implement deleteCombo logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Combo> getCombosByPageAndStatus(int page, int itemsPerPage, String status) {
        return comboRepository.findByStatus(status, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getComboCountByStatus(String status) {
        return comboRepository.countByStatus(status);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Combo> findByKeyword(String keyword, int page, int itemsPerPage) {
        return comboRepository.findByKeyword(keyword, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalCombosByKeyword(String keyword) {
        return 0;
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Combo> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage) {
        return comboRepository.findByKeywordAndStatus(keyword, status, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalCombosByKeywordAndStatus(String keyword, String status) {
        return 0;
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Combo> getAvailableCombos() {
        return comboRepository.findAvailableCombos(PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }
}
