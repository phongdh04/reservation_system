package com.example.qlnh.services;

import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.*;
import com.example.qlnh.repositories.*;
import com.example.qlnh.services.interfaces.IReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService implements IReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final FoodRepository foodRepository;
    private final ComboRepository comboRepository;

    // TODO: VIET LOGIC - tao reservation, tao user neu chua co,
    //       kiem tra gio hoat dong, tim ban trong, tinh tong tien, luu reservation_foods/reservation_combos
    @Override
    @Transactional
    public boolean createReservation(String name, String email, String phone, String date, String time,
                                    int numberOfPeople, String orderDetails, String orderType, String orderId) {
        throw new UnsupportedOperationException("TODO: Implement createReservation logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findByStatusNotOrderByReservationAtAsc("cancelled");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Reservation> getReservations(int page, int itemsPerPage) {
        Pageable pageable = PageRequest.of(page - 1, itemsPerPage, Sort.by("id").descending());
        return reservationRepository.findAllWithGraph(pageable);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Reservation> findByKeyword(String keyword, int page, int itemsPerPage) {
        Pageable pageable = PageRequest.of(page - 1, itemsPerPage, Sort.by("id").descending());
        return reservationRepository.findByKeyword(keyword, pageable);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Reservation> getReservationsByStatus(int page, int itemsPerPage, String status) {
        Pageable pageable = PageRequest.of(page - 1, itemsPerPage, Sort.by("id").descending());
        return reservationRepository.findByStatus(status, pageable);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<Reservation> findByKeywordAndStatus(String keyword, String status, int page, int itemsPerPage) {
        Pageable pageable = PageRequest.of(page - 1, itemsPerPage, Sort.by("id").descending());
        return reservationRepository.findByKeywordAndStatus(keyword, status, pageable);
    }

    // TODO: VIET LOGIC - tim kiem phuc hop voi keyword, status, date
    @Override
    @Transactional(readOnly = true)
    public Page<Reservation> searchReservations(String keyword, String status, String dateStr, int page, int itemsPerPage) {
        throw new UnsupportedOperationException("TODO: Implement searchReservations logic");
    }

    // TODO: VIET LOGIC - gan ban cho reservation, cap nhat trang thai
    @Override
    @Transactional
    public boolean assignTable(Long reservationId, Long tableId) {
        throw new UnsupportedOperationException("TODO: Implement assignTable logic");
    }

    // TODO: VIET LOGIC - huy reservation (set status = cancelled)
    @Override
    @Transactional
    public void deleteReservationById(Long reservationId) {
        throw new UnsupportedOperationException("TODO: Implement deleteReservationById (cancel) logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findByStatusNot(String status) {
        return reservationRepository.findByStatusNot(status);
    }

    // TODO: VIET LOGIC - tinh tong doanh thu
    @Override
    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        Double totalRevenue = reservationRepository.getTotalRevenue();
        return totalRevenue != null ? totalRevenue : 0.0;
    }
}
