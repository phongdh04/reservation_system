package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.enums.ComboStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long> {

        // ==========================================
        // 1. NHÓM KIỂM TRA (Dùng cho luồng Create/Update)
        // ==========================================
        boolean existsByName(String name);

        // ==========================================
        // 2. SIÊU HÀM TÌM KIẾM & PHÂN TRANG (All-in-One)
        // ==========================================
        // Dùng EntityGraph để chống N+1 Query. Thay thế cho toàn bộ các hàm findBy...
        // lắt nhắt
        @EntityGraph(attributePaths = { "comboFoods", "comboFoods.food" })
        @Query("SELECT c FROM Combo c WHERE " +
                        "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:status IS NULL OR c.status = :status)")
        Page<Combo> searchCombos(@Param("keyword") String keyword,
                        @Param("status") ComboStatus status,
                        Pageable pageable);

        // ==========================================
        // 3. NHÓM TRUY VẤN NGHIỆP VỤ HÓA ĐƠN (ORDER/BILL)
        // ==========================================
        // Tự động sinh SQL: Tìm danh sách Combo theo tên và kiểm tra xem có đang bán
        // không
        // (Đã sửa lại để dùng Enum thay vì hardcode chữ 'available')
        List<Combo> findByNameInAndStatus(List<String> names, ComboStatus status);
}