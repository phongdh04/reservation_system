package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.ComboResponse;
import com.example.qlnh.exception.BusinessValidationException;
import com.example.qlnh.dto.request.ComboRequest;
import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.entities.ComboFood;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.services.interfaces.IComboService;
import com.example.qlnh.helpers.CurrentUserResolver;
import jakarta.validation.Valid;
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
    private final CurrentUserResolver currentUserResolver;

    // ─── GET /api/v1/admin/combos ──────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComboResponse>>> listCombos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        // Đẩy toàn bộ tham số xuống Service xử lý
        Page<ComboResponse> comboPage = comboService.getAllCombos(keyword, status, page, itemsPerPage);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Combo thành công", comboPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComboResponse>> getCombo(@PathVariable Long id) {
        // TODO: Lay chi tiet combo + combo_foods
        throw new UnsupportedOperationException("TODO: Implement getCombo logic");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ComboResponse>> createCombo(@Valid @RequestBody ComboRequest request) {

        // 1. Lấy thông tin User đang thao tác (Giữ nguyên Audit Log giống bảng Food)
        User currentUser = currentUserResolver.resolve();
        if (currentUser == null) {
            throw new BusinessValidationException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập lại!");
        }

        // 2. Giao việc cho Service (Nhớ truyền thêm currentUser xuống)
        Combo saved = comboService.createCombo(request, currentUser);

        log.info("[Combo] Created id={} name='{}' by={}", saved.getId(), saved.getName(), currentUser.getEmail());

        // 3. Trả về kết quả (Đổi sang tiếng Việt và dùng static method fromEntity cho
        // đồng bộ)
        return ResponseEntity.ok(ApiResponse.success("Tạo Combo thành công", ComboResponse.fromEntity(saved)));
    }

    // ─── PUT /api/v1/admin/combos/{id} ────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ComboResponse>> updateCombo(
            @PathVariable Long id,
            @Valid @RequestBody ComboRequest request) { // <--- Bắt buộc có @Valid

        // 1. Xác thực người thao tác
        User currentUser = currentUserResolver.resolve();
        if (currentUser == null) {
            throw new BusinessValidationException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập lại!");
        }

        // 2. Giao việc cho Service (Nhớ truyền thêm id và currentUser)
        Combo updated = comboService.updateCombo(id, request, currentUser);

        // 3. Ghi Log và Trả về kết quả
        log.info("[Combo] Updated id={} by={}", id, currentUser.getEmail());

        // Dùng tĩnh hàm fromEntity thay cho toResponse() tự chế
        return ResponseEntity.ok(ApiResponse.success("Cập nhật Combo thành công", ComboResponse.fromEntity(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCombo(@PathVariable Long id) {

        // Giao việc xóa cho Service
        comboService.deleteCombo(id);

        log.info("[Combo] Deleted id={}", id);
        return ResponseEntity.ok(ApiResponse.success("Xóa Combo thành công"));
    }
}
