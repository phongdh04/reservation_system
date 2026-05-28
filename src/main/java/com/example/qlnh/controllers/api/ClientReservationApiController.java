package com.example.qlnh.controllers.api;

import com.example.qlnh.dto.request.CreateReservationRequest;
import com.example.qlnh.dto.request.ReservationQueueRequest;
import com.example.qlnh.dto.response.ApiResponse;
import com.example.qlnh.helpers.ReservationPayloadResolver;
import com.example.qlnh.helpers.ReservationRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/client/reservations")
@RequiredArgsConstructor
public class ClientReservationApiController {

    private final ReservationPayloadResolver payloadResolver;
    private final ReservationRequestValidator validator;

    // TODO: VIET LOGIC - Khi client gui yeu cau dat ban:
    // 1. Resolve user info tu JWT (neu da login)
    // 2. Resolve datetime tu reservationAt (neu co)
    // 3. Resolve orderDetails (gan mac dinh neu trong)
    // 4. Validate request (cac truong bat buoc, gio hoat dong 6h-22h)
    // 5. Check & deduct inventory tu Redis
    // 6. Enqueue vao Redis queue
    // 7. Tra ve orderId
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication auth) {
        throw new UnsupportedOperationException("TODO: Implement createReservation logic for clients");
    }
}
