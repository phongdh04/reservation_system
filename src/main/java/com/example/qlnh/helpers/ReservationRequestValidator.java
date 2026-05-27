package com.example.qlnh.helpers;

import com.example.qlnh.dto.request.CreateReservationRequest;
import com.example.qlnh.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReservationRequestValidator {

    private static final int CLOSED_HOUR_START = 22;
    private static final int CLOSED_HOUR_END = 6;

    // TODO: Kiem tra cac truong bat buoc va gio hoat dong
    public ResponseEntity<ApiResponse<Map<String, String>>> validate(CreateReservationRequest request) {
        ResponseEntity<ApiResponse<Map<String, String>>> missingFields = validateRequiredFields(request);
        if (missingFields != null) return missingFields;
        return validateBusinessHours(request.getTime());
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> validateRequiredFields(CreateReservationRequest req) {
        if (isBlank(req.getName()) || isBlank(req.getEmail()) || isBlank(req.getPhone())
                || isBlank(req.getDate()) || isBlank(req.getTime())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Vui long dien day du thong tin (ten, email, so dien thoai, ngay, gio)!"));
        }
        return null;
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> validateBusinessHours(String time) {
        try {
            int hour = Integer.parseInt(time.split(":")[0]);
            if (hour >= CLOSED_HOUR_START || hour < CLOSED_HOUR_END) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Nha hang khong phuc vu trong khung gio 22:00 - 06:00. Vui long chon gio khac!"));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Dinh dang gio khong hop le. Vui long nhap theo dinh dang HH:mm."));
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
