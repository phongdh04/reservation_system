package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.CreateReservationRequest;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.dto.response.ReservationResponse;
import com.example.qlnh.models.entities.Reservation;
import com.example.qlnh.services.interfaces.IReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

    private final IReservationService reservationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> listReservations(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "date", required = false) String date) {
        // TODO: Lay danh sach reservation voi filter/search
        throw new UnsupportedOperationException("TODO: Implement listReservations logic");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReservation(@Valid @RequestBody CreateReservationRequest req) {
        // TODO: Tao reservation (admin tao thu cong)
        throw new UnsupportedOperationException("TODO: Implement createReservation logic");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
        // TODO: Huy reservation
        throw new UnsupportedOperationException("TODO: Implement cancelReservation logic");
    }

    @PutMapping("/{id}/assign-table")
    public ResponseEntity<ApiResponse<Void>> assignTable(@PathVariable Long id, @RequestBody Map<String, Long> payload) {
        // TODO: Gan ban cho reservation
        throw new UnsupportedOperationException("TODO: Implement assignTable logic");
    }
}
