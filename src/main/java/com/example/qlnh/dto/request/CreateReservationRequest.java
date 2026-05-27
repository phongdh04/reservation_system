package com.example.qlnh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateReservationRequest {
    private String name;

    @Email(message = "Email is not valid")
    private String email;

    private String phone;
    private String date;
    private String time;
    private String reservationAt;

    @Min(value = 1, message = "Number of people must be at least 1")
    private int numberOfPeople = 2;

    private String orderDetails;
    private String note;
    private String orderType = "food";
}
