package com.example.qlnh.models.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(
    name = "reservations",
    indexes = {
        @Index(name = "idx_res_status_reserved_at", columnList = "status, reservation_at"),
        @Index(name = "idx_res_customer_id",        columnList = "customer_id"),
        @Index(name = "idx_res_table_id",           columnList = "table_id"),
        @Index(name = "idx_res_created_at",         columnList = "created_at")
    }
)
@RequiredArgsConstructor
@Getter
@Setter
public class Reservation extends AuditableEntity {

    @Column(name = "total_people", nullable = false)
    private Integer totalPeople;

    @Column(nullable = false)
    private String status;

    @Column(name = "reservation_at", nullable = false)
    private Timestamp reservationAt;

    private String note;

    @Column(name = "order_id", unique = true)
    private String orderId;

    @Column(name = "total_price")
    private Float totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private Table table;
}
