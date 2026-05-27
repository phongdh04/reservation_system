package com.example.qlnh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationQueueRequest {
    private String orderId;
    private String name;
    private String email;
    private String phone;
    private String date;
    private String time;
    private int numberOfPeople;
    private String orderDetails;
    private String orderType;
    private long enqueuedAt;
}
