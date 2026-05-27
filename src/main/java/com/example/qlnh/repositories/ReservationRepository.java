package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = { "customer", "table" })
    Optional<Reservation> findByOrderId(String orderId);

    @EntityGraph(attributePaths = { "customer", "table" })
    Page<Reservation> findByCustomerId(int customerId, Pageable pageable);

    @EntityGraph(attributePaths = { "customer", "table" })
    Page<Reservation> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = { "customer", "table" })
    @Query("SELECT r FROM Reservation r")
    Page<Reservation> findAllWithGraph(Pageable pageable);

    @EntityGraph(attributePaths = { "customer", "table" })
    @Query("SELECT r FROM Reservation r WHERE LOWER(r.customer.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.customer.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.note) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Reservation> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = { "customer", "table" })
    @Query("SELECT r FROM Reservation r WHERE (LOWER(r.customer.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.customer.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.note) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND r.status = :status")
    Page<Reservation> findByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.customer LEFT JOIN FETCH r.table WHERE r.status <> :status ORDER BY r.reservationAt ASC")
    List<Reservation> findByStatusNotOrderByReservationAtAsc(@Param("status") String status);

    @Query("SELECT SUM(r.totalPrice) FROM Reservation r WHERE r.status = 'cancelled'")
    Double getTotalRevenue();

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = :status")
    long countByStatus(@Param("status") String status);
}
