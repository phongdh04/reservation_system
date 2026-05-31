package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.TableRequest;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.TableResponse;
import com.example.qlnh.models.entities.Table;
import com.example.qlnh.services.interfaces.ITableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tables")
@RequiredArgsConstructor
public class TableApiController {

    private final ITableService tableService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TableResponse>>> listTables(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int itemsPerPage,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        // Ném toàn bộ tham số xuống Service xử lý
        Page<TableResponse> tablePage = tableService.getAllTables(keyword, status, page, itemsPerPage);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bàn thành công", tablePage));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> createTable(@Valid @RequestBody TableRequest request) {

        // Ném toàn bộ Request xuống cho Service xử lý và nhận lại Entity đã lưu
        Table savedTable = tableService.createTable(request);

        log.info("[Table] Created id={} name='{}'", savedTable.getId(), savedTable.getName());

        // Map sang Response DTO và trả về
        return ResponseEntity.ok(ApiResponse.success("Tạo bàn thành công", TableResponse.fromEntity(savedTable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody TableRequest request) {

        // Ném ID và Request xuống cho Service lo toàn bộ (Tìm kiếm, Validate, Map data)
        Table updatedTable = tableService.updateTable(id, request);

        log.info("[Table] Updated id={}", id);
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật bàn thành công", TableResponse.fromEntity(updatedTable)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable Long id) {

        // Ném ID cho Service xử lý
        tableService.deleteTable(id);

        log.info("[Table] Deleted id={}", id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bàn thành công"));
    }
}
