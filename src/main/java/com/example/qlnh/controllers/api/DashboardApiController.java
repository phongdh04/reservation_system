package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.services.interfaces.IReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class DashboardApiController {

    private final IReservationService reservationService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueStats() {
        // TODO: Tra ve doanh thu (totalRevenue + monthly)
        throw new UnsupportedOperationException("TODO: Implement getRevenueStats logic");
    }
}
