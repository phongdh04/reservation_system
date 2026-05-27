package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.ComboResponse;
import com.example.qlnh.dto.request.ComboRequest;
import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.entities.ComboFood;
import com.example.qlnh.services.interfaces.IComboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/combos")
@RequiredArgsConstructor
public class ComboApiController {

    private final IComboService comboService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComboResponse>>> listCombos(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        // TODO: Lay danh sach combo voi filter
        throw new UnsupportedOperationException("TODO: Implement listCombos logic");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComboResponse>> getCombo(@PathVariable Long id) {
        // TODO: Lay chi tiet combo + combo_foods
        throw new UnsupportedOperationException("TODO: Implement getCombo logic");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ComboResponse>> createCombo(@RequestBody ComboRequest request) {
        // TODO: Tao combo + combo_foods
        throw new UnsupportedOperationException("TODO: Implement createCombo logic");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ComboResponse>> updateCombo(@PathVariable Long id, @RequestBody ComboRequest request) {
        // TODO: Cap nhat combo + combo_foods
        throw new UnsupportedOperationException("TODO: Implement updateCombo logic");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCombo(@PathVariable Long id) {
        // TODO: Xoa combo (kiem tra reservation_combos)
        throw new UnsupportedOperationException("TODO: Implement deleteCombo logic");
    }
}
