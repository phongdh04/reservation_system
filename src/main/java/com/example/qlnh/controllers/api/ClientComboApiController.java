package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.ComboResponse;
import com.example.qlnh.models.entities.Combo;
import com.example.qlnh.models.entities.ComboFood;
import com.example.qlnh.services.interfaces.IComboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/client/combos")
@RequiredArgsConstructor
public class ClientComboApiController {

    private final IComboService comboService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComboResponse>>> listActiveCombos(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "12") int itemsPerPage) {
        // TODO: Lay danh sach combo active cho khach hang
        throw new UnsupportedOperationException("TODO: Implement listActiveCombos logic");
    }
}
