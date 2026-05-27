package com.example.qlnh.services.interfaces;

import com.example.qlnh.models.entities.Reservation;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface IReservationService {

    boolean createReservation(String name, String email, String phone, String date, String time,
                             int numberOfPeople, String orderDetails, String orderType, String orderId);
    List<Reservation> getAllReservations();
    Page<Reservation> getReservations(int page, int itemsPerPage);
    Page<Reservation> findByKeyword(String keyword, int page, int itemsPerPage);
    Page<Reservation> getReservationsByStatus(int page, int itemsPerPage, String status);
    Page<Reservation> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage);
    Page<Reservation> searchReservations(String keyword, String status, String date, int page, int itemsPerPage);
    Reservation getReservationById(Long id);
    boolean assignTable(Long reservationId, Long tableId);
    void deleteReservationById(Long reservationId);
    List<Reservation> findByStatusNot(String status);
    Double getTotalRevenue();
}
