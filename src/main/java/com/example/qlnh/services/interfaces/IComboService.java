package com.example.qlnh.services.interfaces;

import com.example.qlnh.dto.request.ComboRequest;
import com.example.qlnh.dto.response.ComboResponse;
import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.entities.User;
import org.springframework.data.domain.Page;

public interface IComboService {

    // 1. Hàm lấy danh sách và phân trang
    Page<ComboResponse> getAllCombos(String keyword, String status, int page, int itemsPerPage);

    // 2. Hàm tạo mới
    Combo createCombo(ComboRequest request, User currentUser);

    // 3. Hàm cập nhật
    Combo updateCombo(Long id, ComboRequest request, User currentUser);

    // 4. Hàm xóa (Xóa mềm)
    void deleteCombo(Long id);

}