package com.example.qlnh.services.interfaces;

import com.example.qlnh.models.entities.Reservation;
import com.example.qlnh.models.entities.Combo;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IComboService {

    List<Combo> getAllCombos();
    Page<Combo> getCombosByPage(int page, int itemsPerPage);
    long getTotalCombos();
    Combo getComboById(Long id);
    Combo createCombo(Combo combo);
    Combo updateCombo(Combo combo);
    Combo createComboWithFoods(com.example.qlnh.dto.request.ComboRequest request);
    Combo updateComboWithFoods(Long id, com.example.qlnh.dto.request.ComboRequest request);
    List<com.example.qlnh.models.entities.ComboFood> getComboFoods(Long comboId);
    boolean deleteCombo(Long id);
    Page<Combo> getCombosByPageAndStatus(int page, int itemsPerPage, String status);
    long getComboCountByStatus(String status);
    Page<Combo> findByKeyword(String keyword, int page, int itemsPerPage);
    long getTotalCombosByKeyword(String keyword);
    Page<Combo> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage);
    long getTotalCombosByKeywordAndStatus(String keyword, String status);
    List<Combo> getAvailableCombos();
}
