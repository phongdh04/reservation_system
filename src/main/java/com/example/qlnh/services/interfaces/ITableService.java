package com.example.qlnh.services.interfaces;

import com.example.qlnh.dto.request.TableRequest;
import com.example.qlnh.dto.response.TableResponse; // Nhớ import DTO này nhé
import com.example.qlnh.models.entities.Table;
import org.springframework.data.domain.Page;

public interface ITableService {

    // ==========================================
    // 1. SIÊU HÀM TÌM KIẾM & PHÂN TRANG (All-in-One)
    // ==========================================
    // Thay thế cho tất cả các hàm get, find, count dư thừa
    Page<TableResponse> getAllTables(String keyword, String status, int page, int itemsPerPage);

    // ==========================================
    // 2. NHÓM HÀM THAO TÁC CƠ BẢN (CRUD)
    // ==========================================

    Table getTableById(Long id);

    Table createTable(TableRequest request);

    Table updateTable(Long id, TableRequest request);

    void deleteTable(Long id);
}