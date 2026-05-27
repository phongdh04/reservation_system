package com.example.qlnh.dto.response;

import com.example.qlnh.models.entities.Reservation;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class ReservationResponse {
    private Long id;
    private Integer totalPeople;
    private String status;
    private Timestamp reservationAt;
    private String note;
    private Float totalPrice;
    private Timestamp createdAt;
    private String customerName;
    private String customerPhone;
    private String tableName;
    private Long tableId;

    public static ReservationResponse fromEntity(Reservation reservation) {
        if (reservation == null) return null;
        return ReservationResponse.builder()
                .id(reservation.getId())
                .totalPeople(reservation.getTotalPeople())
                .status(reservation.getStatus())
                .reservationAt(reservation.getReservationAt())
                .note(reservation.getNote())
                .totalPrice(reservation.getTotalPrice())
                .createdAt(reservation.getCreatedAt())
                .customerName(reservation.getCustomer() != null ? reservation.getCustomer().getName() : null)
                .customerPhone(reservation.getCustomer() != null ? reservation.getCustomer().getPhone() : null)
                .tableName(reservation.getTable() != null ? reservation.getTable().getName() : null)
                .tableId(reservation.getTable() != null ? reservation.getTable().getId() : null)
                .build();
    }
}
