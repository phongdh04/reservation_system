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

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getAllTables() {
        // TODO: Lay tat ca ban
        throw new UnsupportedOperationException("TODO: Implement getAllTables logic");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TableResponse>>> listTables(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        // TODO: Lay danh sach ban voi filter
        throw new UnsupportedOperationException("TODO: Implement listTables logic");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> createTable(@Valid @RequestBody TableRequest req) {
        // TODO: Tao ban moi
        throw new UnsupportedOperationException("TODO: Implement createTable logic");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(@PathVariable Long id, @Valid @RequestBody TableRequest req) {
        // TODO: Cap nhat ban
        throw new UnsupportedOperationException("TODO: Implement updateTable logic");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable Long id) {
        // TODO: Xoa ban (soft delete)
        throw new UnsupportedOperationException("TODO: Implement deleteTable logic");
    }
}
